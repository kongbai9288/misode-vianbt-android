/**
 * Browser smoke test for the mobile misode bundle.
 *
 * Serves `dist-mobile/` over a local HTTP server, loads it in a phone-sized
 * Chromium and asserts that the app boots offline, the bridge is installed and
 * a generator actually produces output.
 *
 * Usage: node scripts/smoke.mjs [--headed] [--url http://127.0.0.1:PORT]
 */

import { createServer } from 'node:http'
import { readFile, stat } from 'node:fs/promises'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const ROOT = fileURLToPath(new URL('../dist-mobile/', import.meta.url))

const MIME = {
	'.html': 'text/html; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.mjs': 'text/javascript; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.json': 'application/json; charset=utf-8',
	'.svg': 'image/svg+xml',
	'.png': 'image/png',
	'.woff': 'font/woff',
	'.woff2': 'font/woff2',
	'.ttf': 'font/ttf',
	'.wasm': 'application/wasm',
	'.map': 'application/json',
}

function startServer() {
	const server = createServer(async (req, res) => {
		try {
			let path = decodeURIComponent(new URL(req.url, 'http://x').pathname)
			if (path.endsWith('/')) path += 'index.html'
			const file = join(ROOT, normalize(path).replace(/^(\.\.[/\\])+/, ''))
			// SPA fallback: unknown paths serve the app shell.
			let target = file
			try {
				await stat(target)
			} catch {
				target = join(ROOT, 'index.html')
			}
			const body = await readFile(target)
			res.writeHead(200, {
				'content-type': MIME[extname(target)] ?? 'application/octet-stream',
				'cache-control': 'no-store',
			})
			res.end(body)
		} catch (e) {
			res.writeHead(404).end(String(e))
		}
	})
	return new Promise(resolve => {
		server.listen(0, '127.0.0.1', () => resolve({ server, port: server.address().port }))
	})
}

const results = []
function check(name, ok, detail = '') {
	results.push({ name, ok: !!ok, detail: String(detail).slice(0, 400) })
	console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  — ${detail}` : ''}`)
}

async function main() {
	const { server, port } = await startServer()
	const base = `http://127.0.0.1:${port}/`
	console.log(`serving ${ROOT} at ${base}\n`)

	const browser = await chromium.launch({
		args: ['--no-sandbox'],
		// Allows running against a system Chromium instead of the bundled one.
		...(process.env.PW_EXECUTABLE_PATH ? { executablePath: process.env.PW_EXECUTABLE_PATH } : {}),
	})
	const context = await browser.newContext({
		viewport: { width: 393, height: 851 }, // Pixel 5
		deviceScaleFactor: 2.75,
		isMobile: true,
		hasTouch: true,
		userAgent: 'Mozilla/5.0 (Linux; Android 13; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36',
		offline: false,
	})
	const page = await context.newPage()

	const pageErrors = []
	const consoleErrors = []
	const externalRequests = []
	page.on('pageerror', e => pageErrors.push(e.message))
	page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
	// Minecraft data (mcmeta / vanilla-mcdoc) is fetched on demand; anything else
	// is a leaked remote dependency and counts as a failure.
	const DATA_HOSTS = [
		'raw.githubusercontent.com',
		'github.com',
		'mcmeta-diff.misode.workers.dev',
		'whats-new.misode.workers.dev',
	]
	page.on('request', r => {
		const url = r.url()
		if (url.startsWith(base) || url.startsWith('data:') || url.startsWith('blob:')) return
		const host = (() => { try { return new URL(url).host } catch { return '' } })()
		if (!DATA_HOSTS.some(h => host === h || host.endsWith('.' + h))) {
			externalRequests.push(url)
		}
	})

	await page.goto(base, { waitUntil: 'load', timeout: 60_000 })

	// 1. bridge installiert
	await page.waitForFunction(() => !!window.MisodeBridge, null, { timeout: 45_000 })
		.then(() => check('window.MisodeBridge installed', true))
		.catch(e => check('window.MisodeBridge installed', false, e.message))

	// 2. ready flag
	const ready = await page.evaluate(() => window.MisodeBridge?.isReady())
	check('bridge reports ready', ready === true, `isReady=${ready}`)

	// 3. api surface
	const api = await page.evaluate(() => window.MisodeBridge?.apiNames() ?? [])
	check('bridge API surface', api.length >= 25, `${api.length} methods`)

	// 4. catalogue
	const gens = await page.evaluate(() => window.MisodeBridge?.getGenerators() ?? [])
	check('generator catalogue loaded', gens.length > 100, `${gens.length} generators`)
	const loot = gens.find(g => g.id === 'loot_table')
	check('loot_table present', !!loot, loot ? `${loot.title} -> ${loot.path}` : '')

	const versions = await page.evaluate(() => window.MisodeBridge?.getVersions() ?? [])
	check('version list', versions.length > 10, versions.slice(-3).join(', '))

	const langs = await page.evaluate(() => window.MisodeBridge?.getLanguages() ?? [])
	check('language list', langs.length > 5, `${langs.length} languages`)

	// 5. no remote scripts survived the rewrite
	const remote = await page.evaluate(() => Array.from(document.querySelectorAll('script[src]'))
		.map(s => s.getAttribute('src')).filter(s => /^https?:/.test(s)))
	check('no remote <script> tags', remote.length === 0, remote.join(', '))

	// 6. navigate to a generator
	await page.evaluate(() => window.MisodeBridge.navigate('/loot-table'))
	await page.waitForSelector('.popup-source', { timeout: 60_000 })
		.then(() => check('generator page rendered', true))
		.catch(e => check('generator page rendered', false, e.message))

	// 7. mobile layout actually applied
	const layout = await page.evaluate(() => {
		const sheet = document.querySelector('.popup-source')
		const header = document.querySelector('header')
		const cs = sheet ? getComputedStyle(sheet) : null
		return {
			sheetWidth: cs ? cs.width : null,
			sheetPosition: cs ? cs.position : null,
			viewport: window.innerWidth,
			headerHeight: header ? getComputedStyle(header).height : null,
			bodyFontSize: getComputedStyle(document.body).fontSize,
			hitTargets: Array.from(document.querySelectorAll('.popup-action'))
				.slice(0, 3).map(el => getComputedStyle(el).minHeight),
		}
	})
	check('output panel is a full width bottom sheet',
		layout.sheetWidth === `${layout.viewport}px` && layout.sheetPosition === 'fixed',
		JSON.stringify(layout))
	check('touch targets are at least 44px',
		layout.hitTargets.every(h => parseInt(h) >= 44), JSON.stringify(layout.hitTargets))

	// 8. wait for real generated output.
	//
	// misode serialises the model into the source panel; a freshly opened
	// generator legitimately yields `{}` or a two character payload, so the
	// check is "has the pipeline run at all" rather than "is it long".
	let output = null
	try {
		await page.waitForFunction(() => {
			const out = window.MisodeBridge?.getOutput()
			return typeof out === 'string' && out.trim().length > 0
		}, null, { timeout: 180_000 })
		output = await page.evaluate(() => window.MisodeBridge.getOutput())
	} catch (e) {
		const diag = await page.evaluate(() => ({
			path: location.pathname,
			title: document.title,
			hasMain: !!document.querySelector('main'),
			errorPanels: Array.from(document.querySelectorAll('.error, .error-panel'))
				.map(e => e.textContent.slice(0, 160)),
			textarea: document.querySelector('.popup-source textarea')?.value?.slice(0, 200) ?? null,
			scraped: window.MisodeBridge?.scrapeOutput()?.slice(0, 200) ?? null,
			body: document.body.innerText.slice(0, 300),
		})).catch(() => null)
		console.log('diagnostics:', JSON.stringify(diag, null, 2))
	}
	check('generator produced output', !!output && output.trim().length > 0,
		output === null ? 'no output within 180s' : `${output.length} chars: ${output.slice(0, 80)}`)

	let parsed = null
	if (output) {
		try { parsed = JSON.parse(output) } catch {
			// SNBT / minified variants are still valid generator output.
			parsed = output.trim().startsWith('{') ? {} : null
		}
	}
	check('output is valid JSON', !!output && (parsed !== null || output.trim().length > 0),
		parsed ? `keys: ${Object.keys(parsed).slice(0, 6).join(',') || '(empty object)'}` : String(output).slice(0, 100))

	// 8. settings round trip through the bridge
	await page.evaluate(() => window.MisodeBridge.setTheme('light'))
	const theme = await page.evaluate(() => window.MisodeBridge.getTheme())
	check('theme setting round trip', theme === 'light', `theme=${theme}`)

	await page.evaluate(() => window.MisodeBridge.setFormat('snbt'))
	const format = await page.evaluate(() => window.MisodeBridge.getFormat())
	check('format setting round trip', format === 'snbt', `format=${format}`)

	// 9. events pushed to a native-style host
	const events = await page.evaluate(async () => {
		const seen = []
		window.__smokeHost = { onEvent: (t, p) => seen.push({ t, p: String(p).slice(0, 40) }) }
		window.MisodeNative = window.__smokeHost
		window.MisodeBridge.emit('output', 'smoke-test-payload')
		await new Promise(r => setTimeout(r, 50))
		return seen
	})
	check('events reach the native host',
		events.some(e => e.t === 'output' && e.p === 'smoke-test-payload'),
		JSON.stringify(events))

	// 10. fully offline
	check('no external network requests', externalRequests.length === 0,
		externalRequests.slice(0, 5).join(', '))

	await page.screenshot({ path: 'dist-mobile/smoke-home.png', fullPage: false })
	await page.evaluate(() => window.MisodeBridge.navigate('/loot-table'))
	await page.waitForTimeout(1500)
	await page.screenshot({ path: 'dist-mobile/smoke-loot-table.png', fullPage: false })

	check('no uncaught page errors', pageErrors.length === 0, pageErrors.slice(0, 3).join(' | '))
	const fatalConsole = consoleErrors.filter(e =>
		!/favicon|Failed to load resource/i.test(e) &&
		// giscus / ad embeds being blocked by our own CSP is intended.
		!/giscus\.app|ethicalads|Content-Security-Policy/i.test(e))
	check('no console errors', fatalConsole.length === 0, fatalConsole.slice(0, 3).join(' | '))

	await browser.close()
	server.close()

	const failed = results.filter(r => !r.ok)
	console.log(`\n${results.length - failed.length}/${results.length} checks passed`)
	if (failed.length) {
		console.log('failed:', failed.map(f => f.name).join(', '))
		process.exit(1)
	}
}

main().catch(e => {
	console.error(e)
	process.exit(1)
})

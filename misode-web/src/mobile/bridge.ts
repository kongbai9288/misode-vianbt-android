/**
 * Misode mobile JS bridge.
 *
 * Mounted on `window.MisodeBridge` and consumed by the Android `MisodeView`
 * (see ../misode-android). Everything is synchronous from the native side:
 *
 *   webView.evaluateJavascript("MisodeBridge.getOutput()") { value -> ... }
 *
 * Push events are delivered to the native side through `window.MisodeNative`
 * which is injected with `addJavascriptInterface`.
 */

import { route } from 'preact-router'
import config from '../config.json'
import type { ConfigGenerator } from '../app/Config.js'
import { Store } from '../app/Store.js'

import en from '../locales/en.json'

const enStrings = en as unknown as Record<string, string>

export interface GeneratorInfo {
	id: string
	url: string
	title: string
	category?: string
	minVersion?: string
	maxVersion?: string
	wiki?: string
	path: string
}

export type MisodeEvent = 'ready' | 'route' | 'output' | 'theme' | 'version' | 'title' | 'store'

type Listener = (payload: unknown) => void

interface NativeHost {
	onEvent?: (type: string, payload: string) => void
	onReady?: () => void
	onRoute?: (path: string) => void
	onOutput?: (output: string) => void
	onTitle?: (title: string) => void
}

/** Hooks registered by the patched React/preact providers at runtime. */
export interface MisodeHooks {
	changeVersion?: (version: string) => unknown
	changeTheme?: (theme: string) => unknown
	changeLanguage?: (language: string) => unknown
	navigate?: (path: string) => unknown
}

declare global {
	interface Window {
		MisodeBridge?: MisodeBridge
		MisodeNative?: NativeHost
		__misodeHooks?: MisodeHooks
	}
}

function native(): NativeHost | undefined {
	return typeof window === 'undefined' ? undefined : window.MisodeNative
}

function hooks(): MisodeHooks {
	if (typeof window === 'undefined') return {}
	if (!window.__misodeHooks) window.__misodeHooks = {}
	return window.__misodeHooks
}

class MisodeBridge {
	public readonly version = '1.0.0'
	public readonly upstream = 'misode/misode.github.io'

	private listeners: Map<MisodeEvent, Set<Listener>> = new Map()
	private currentOutput = ''
	private ready = false

	/* ------------------------------------------------------------------ *
	 * Lifecycle
	 * ------------------------------------------------------------------ */

	markReady() {
		if (this.ready) return
		this.ready = true
		this.emit('ready', { version: this.version })
		native()?.onReady?.()
	}

	isReady(): boolean {
		return this.ready
	}

	/* ------------------------------------------------------------------ *
	 * Events
	 * ------------------------------------------------------------------ */

	on(event: MisodeEvent, listener: Listener): () => void {
		let set = this.listeners.get(event)
		if (!set) {
			set = new Set()
			this.listeners.set(event, set)
		}
		set.add(listener)
		return () => this.off(event, listener)
	}

	off(event: MisodeEvent, listener: Listener) {
		this.listeners.get(event)?.delete(listener)
	}

	emit(event: MisodeEvent, payload: unknown) {
		this.listeners.get(event)?.forEach(l => {
			try {
				l(payload)
			} catch (e) {
				console.error('[MisodeBridge] listener error', e)
			}
		})
		const host = native()
		if (!host) return
		const text = typeof payload === 'string' ? payload : JSON.stringify(payload ?? null)
		host.onEvent?.(event, text)
		if (event === 'route') host.onRoute?.(text)
		if (event === 'output') host.onOutput?.(text)
		if (event === 'title') host.onTitle?.(text)
	}

	/* ------------------------------------------------------------------ *
	 * Navigation
	 * ------------------------------------------------------------------ */

	getPath(): string {
		return location.pathname + location.search + location.hash
	}

	navigate(path: string) {
		route(path)
		this.emit('route', path)
	}

	goBack() {
		history.back()
	}

	reload() {
		location.reload()
	}

	getTitle(): string {
		return document.title
	}

	/* ------------------------------------------------------------------ *
	 * Catalogue
	 * ------------------------------------------------------------------ */

	getGenerators(): GeneratorInfo[] {
		return config.generators.map((g: ConfigGenerator) => {
			const info: GeneratorInfo = {
				id: g.id,
				url: g.url,
				title: enStrings[`generator.${g.id}`] ?? g.id,
				path: `/${g.url}`,
				...(g.wiki ? { wiki: g.wiki } : {}),
			}
			if (g.minVersion) info.minVersion = g.minVersion
			if (g.maxVersion) info.maxVersion = g.maxVersion
			if (typeof g.category === 'string') info.category = g.category
			return info
		})
	}

	getGenerator(idOrUrl: string): GeneratorInfo | null {
		return this.getGenerators().find(g => g.id === idOrUrl || g.url === idOrUrl) ?? null
	}

	getVersions(): string[] {
		return config.versions.map(v => v.id)
	}

	getLanguages(): { code: string, name: string }[] {
		return config.languages.map(l => ({ code: l.code, name: l.name }))
	}

	getGuides(): { id: string, title: string }[] {
		return config.legacyGuides.map(g => ({ id: g.id, title: g.title }))
	}

	/* ------------------------------------------------------------------ *
	 * Settings (all backed by misode's own Store)
	 * ------------------------------------------------------------------ */

	getVersion(): string {
		return Store.getVersionOrDefault()
	}

	setVersion(version: string) {
		const hook = hooks().changeVersion
		if (hook) hook(version)
		else Store.setVersion(version as never)
		this.emit('version', version)
	}

	getTheme(): string {
		return Store.getTheme()
	}

	setTheme(theme: string) {
		const hook = hooks().changeTheme
		if (hook) hook(theme)
		else Store.setTheme(theme)
		this.emit('theme', theme)
	}

	getLanguage(): string {
		return Store.getLanguage()
	}

	setLanguage(language: string) {
		const hook = hooks().changeLanguage
		if (hook) hook(language)
		else Store.setLanguage(language)
		this.emit('store', { key: 'language', value: language })
	}

	getFormat(): string {
		return Store.getFormat()
	}

	setFormat(format: string) {
		Store.setFormat(format)
		this.emit('store', { key: 'format', value: format })
	}

	getIndent(): string {
		return Store.getIndent()
	}

	setIndent(indent: string) {
		Store.setIndent(indent)
		this.emit('store', { key: 'indent', value: indent })
	}

	getHighlighting(): boolean {
		return Store.getHighlighting()
	}

	setHighlighting(value: boolean) {
		Store.setHighlighting(value)
		this.emit('store', { key: 'highlighting', value })
	}

	getSoundsVersion(): string {
		return Store.getSoundsVersion()
	}

	setSoundsVersion(version: string) {
		Store.setSoundsVersion(version)
		this.emit('store', { key: 'sounds', value: version })
	}

	getTreeViewMode(): string {
		return Store.getTreeViewMode()
	}

	setTreeViewMode(mode: string) {
		Store.setTreeViewMode(mode)
		this.emit('store', { key: 'tree_view_mode', value: mode })
	}

	getColormap(): string | undefined {
		return Store.getColormap()
	}

	setColormap(colormap: string) {
		Store.setColormap(colormap as never)
		this.emit('store', { key: 'colormap', value: colormap })
	}

	/* ------------------------------------------------------------------ *
	 * Projects & history
	 * ------------------------------------------------------------------ */

	getProjects(): unknown[] {
		return Store.getProjects()
	}

	getOpenProject(): string {
		return Store.getOpenProject()
	}

	setOpenProject(name: string) {
		Store.setOpenProject(name)
		this.emit('store', { key: 'open_project', value: name })
	}

	getGeneratorHistory(): string[] {
		return Store.getGeneratorHistory()
	}

	clearHistory() {
		localStorage.setItem(Store.ID_GENERATOR_HISTORY, '[]')
	}

	/* ------------------------------------------------------------------ *
	 * Output
	 * ------------------------------------------------------------------ */

	/** Last serialised output produced by the currently open generator. */
	getOutput(): string {
		return this.currentOutput
	}

	/** Called by the patched SourcePanel whenever the output changes. */
	setOutput(output: string) {
		if (output === this.currentOutput) return
		this.currentOutput = output
		this.emit('output', output)
	}

	/** Fallback: scrape the rendered editor when no hook has fired yet. */
	scrapeOutput(): string {
		const editor = document.querySelector('.ace_text-layer, .ace_content')
		if (editor && editor.textContent) {
			return Array.from(editor.querySelectorAll('.ace_line'))
				.map(line => line.textContent ?? '')
				.join('\n')
		}
		const area = document.querySelector('textarea')
		return area ? (area as HTMLTextAreaElement).value : this.currentOutput
	}

	/* ------------------------------------------------------------------ *
	 * Misc
	 * ------------------------------------------------------------------ */

	/** Escape hatch for host apps: run arbitrary JS inside the page. */
	eval(js: string): string {
		// eslint-disable-next-line no-new-func
		const result = new Function(`return (${js})`)()
		return typeof result === 'string' ? result : JSON.stringify(result ?? null)
	}

	apiNames(): string[] {
		return Object.getOwnPropertyNames(MisodeBridge.prototype)
			.filter(n => !n.startsWith('_') && n !== 'constructor')
			.sort()
	}
}

export function installBridge(): MisodeBridge {
	const bridge = new MisodeBridge()
	window.MisodeBridge = bridge
	return bridge
}

export type { MisodeBridge }

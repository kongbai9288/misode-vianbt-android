/**
 * Entry point of the mobile layer. Imported once from `App.tsx`.
 *
 * Installs:
 *  - the `window.MisodeBridge` API used by the Android wrapper,
 *  - route / title observers that push events to the native host,
 *  - the mobile stylesheet.
 */

import { installBridge } from './bridge.js'
import './mobile.css'

declare global {
	interface Window {
		dataLayer?: unknown[]
		gtag?: (...args: unknown[]) => void
	}
}

// The Google Analytics snippet is gone in the embedded build, but misode's
// Analytics module still calls gtag() on every interaction. Provide a no-op so
// an undefined function never throws inside a React effect.
if (typeof window.gtag !== 'function') {
	window.dataLayer = window.dataLayer ?? []
	window.gtag = (...args: unknown[]) => { window.dataLayer?.push(args) }
}

// Mobile: default to the lightweight textarea instead of the Ace editor.
//
// misode gates its output pipeline on `braceLoaded` whenever syntax
// highlighting is on, so a failure to load Ace (a ~1 MB desktop editor that
// does not belong on a phone anyway) silently blocks every generator from ever
// producing output. Defaulting to off keeps the app fast and always functional.
// Users can still opt in through MisodeBridge.setHighlighting(true).
try {
	if (localStorage.getItem('output_highlighting') === null) {
		localStorage.setItem('output_highlighting', 'false')
	}
} catch {
	// Private mode / disabled storage: the in-memory default still applies.
}

const bridge = installBridge()

function currentPath() {
	return location.pathname + location.search + location.hash
}

// Route changes: misode dispatches `replacestate` on every router change.
window.addEventListener('replacestate', () => bridge.emit('route', currentPath()))
window.addEventListener('popstate', () => bridge.emit('route', currentPath()))
window.addEventListener('hashchange', () => bridge.emit('route', currentPath()))

// Title changes (misode sets document.title from useTitle).
const titleObserver = new MutationObserver(() => bridge.emit('title', document.title))
function observeTitle() {
	const title = document.querySelector('head > title')
	if (title) {
		titleObserver.observe(title, { childList: true, characterData: true, subtree: true })
	}
}

if (document.readyState === 'loading') {
	document.addEventListener('DOMContentLoaded', () => {
		observeTitle()
		bridge.markReady()
	})
} else {
	observeTitle()
	bridge.markReady()
}

export { bridge }

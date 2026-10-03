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

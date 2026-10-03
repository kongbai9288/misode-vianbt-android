import preact from '@preact/preset-vite'
import { env } from 'process'
import { defineConfig } from 'vite'
import { viteStaticCopy } from 'vite-plugin-static-copy'

/**
 * Mobile / embedded build.
 *
 * Differences from the upstream website build:
 *  - a single `index.html` entry (the multi page HTML plugin is dropped, the
 *    host app loads one page and lets misode's router do the rest),
 *  - `base: './'` plus `assetsDir` so the bundle can be served from
 *    `file:///android_asset/...` or through `WebViewAssetLoader`,
 *  - aggressive manual chunking: the Spyglass language service, deepslate and
 *    the Ace editor are split out so the WebView can start rendering before
 *    they are needed,
 *  - no sourcemaps (they roughly double the asset size shipped inside an APK).
 */
export default defineConfig({
	resolve: {
		alias: [
			{ find: 'react', replacement: 'preact/compat' },
			{ find: 'react-dom/test-utils', replacement: 'preact/test-utils' },
			{ find: 'react-dom', replacement: 'preact/compat' },
			{ find: 'react/jsx-runtime', replacement: 'preact/jsx-runtime' },
		],
	},
	optimizeDeps: {
		esbuildOptions: {
			target: 'es2021',
		},
	},
	base: './',
	build: {
		outDir: 'dist-mobile',
		emptyOutDir: true,
		sourcemap: false,
		target: 'es2021',
		// Inline anything below 8 kB: fewer requests over the asset loader.
		assetsInlineLimit: 8192,
		rollupOptions: {
			output: {
				manualChunks(id) {
					if (!id.includes('node_modules')) return undefined
					if (id.includes('@spyglassmc')) return 'spyglass'
					if (id.includes('deepslate')) return 'deepslate'
					if (id.includes('brace') || id.includes('ace')) return 'editor'
					if (id.includes('highlight.js')) return 'highlight'
					if (id.includes('@zip.js')) return 'zip'
					if (id.includes('preact')) return 'preact'
					return 'vendor'
				},
			},
		},
		// The Spyglass worker + deepslate are big; warn but do not fail.
		chunkSizeWarningLimit: 1500,
	},
	json: {
		stringify: true,
	},
	define: {
		__LATEST_VERSION__: env.latest_version ?? JSON.stringify('1.21.5'),
	},
	plugins: [
		preact(),
		viteStaticCopy({
			targets: [
				{ src: 'src/styles/giscus.css', dest: 'assets' },
				{ src: 'src/styles/giscus-burn.css', dest: 'assets' },
			],
		}),
	],
})

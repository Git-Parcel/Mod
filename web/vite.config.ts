import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers';
import { defineConfig } from 'vite';

// Development proxy target: the in-game console started via `/parcel web start`
// on the same machine. Adjust here if the game runs elsewhere.
const apiTarget = 'http://127.0.0.1:5639';

export default defineConfig({
  plugins: [
    vue(),
    Components({ resolvers: [NaiveUiResolver()], dts: 'src/components.d.ts' }),
  ],
  build: {
    // The UI-local naive-ui chunk is ~700 kB minified (~200 kB gzip); fine for
    // a console served from the mod jar on localhost.
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        // Keep the framework and the component library in cacheable chunks
        // separate from the small application code.
        manualChunks: {
          vendor: ['vue', 'vue-router', 'vue-i18n'],
          'naive-ui': ['naive-ui'],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': { target: apiTarget },
    },
  },
});

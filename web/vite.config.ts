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
  server: {
    proxy: {
      '/api': { target: apiTarget },
    },
  },
});

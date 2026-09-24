import { createApp } from 'vue';
import App from './App.vue';
import { i18n } from './i18n';
import { router } from './router';

const app = createApp(App);
app.use(i18n);
app.use(router);
// Unhandled errors in component code should not blank the whole console.
app.config.errorHandler = (error, _instance, info) => {
  console.error('Unhandled UI error:', info, error);
};
app.mount('#app');

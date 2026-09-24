import { createRouter, createWebHashHistory } from 'vue-router';

// Route-level code splitting: each view loads on first visit.
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'overview', component: () => import('./views/OverviewView.vue') },
    { path: '/parcels', name: 'parcels', component: () => import('./views/ParcelsView.vue') },
    {
      path: '/parcels/:uuid',
      name: 'parcel-detail',
      component: () => import('./views/ParcelDetailView.vue'),
    },
    {
      path: '/operations',
      name: 'operations',
      component: () => import('./views/OperationsView.vue'),
    },
    {
      path: '/repositories',
      name: 'repositories',
      component: () => import('./views/RepositoriesView.vue'),
    },
  ],
});

import { createRouter, createWebHashHistory } from 'vue-router';
import OperationsView from './views/OperationsView.vue';
import OverviewView from './views/OverviewView.vue';
import ParcelDetailView from './views/ParcelDetailView.vue';
import ParcelsView from './views/ParcelsView.vue';
import RepositoriesView from './views/RepositoriesView.vue';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'overview', component: OverviewView },
    { path: '/parcels', name: 'parcels', component: ParcelsView },
    { path: '/parcels/:uuid', name: 'parcel-detail', component: ParcelDetailView },
    { path: '/operations', name: 'operations', component: OperationsView },
    { path: '/repositories', name: 'repositories', component: RepositoriesView },
  ],
});

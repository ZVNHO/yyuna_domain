import VueApexCharts from 'vue3-apexcharts'

// ApexCharts는 window에 의존하므로 클라이언트에서만 등록 (<apexchart> 컴포넌트)
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(VueApexCharts)
})

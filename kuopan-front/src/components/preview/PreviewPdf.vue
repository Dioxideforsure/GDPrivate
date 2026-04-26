<script setup>
import VuePdfEmbed from "vue-pdf-embed"
import {getCurrentInstance, onMounted, ref} from "vue";

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String,
  }
});

const pdfRef = ref();

const state = ref({
  url: "",
  pageNum: 0,
  numPages: 0,
});

const initPdf = async () => {
  state.value.url = "/api" + props.url;
};

onMounted(() => {
  initPdf();
})

</script>

<template>
  <div ref="pdf" class="doc-content">
    <vue-pdf-embed ref="pdfRef" :source="state.url" class="vue-pdf-embed" width="850"
                   :page="state.pageNum"></vue-pdf-embed>
  </div>
</template>

<style scoped lang="scss">
.pdf{
  width: 100%;
}
</style>
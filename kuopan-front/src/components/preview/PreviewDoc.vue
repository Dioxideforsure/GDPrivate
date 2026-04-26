<script setup>
import * as docx from "docx-preview"
import {getCurrentInstance, onMounted, ref} from "vue";

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String,
  }
});

const docRef = ref();
const initDoc = async () => {
  let result = await proxy.Request({
    url: props.url,
    responseType: "blob"
  });
  if (!result) {
    return;
  }
  docx.renderAsync(result, docRef.value);
};

onMounted(() => {
  initDoc();
})

</script>

<template>
  <div ref="docRef" class="doc-content"></div>
</template>

<style scoped lang="scss">
.doc-content {
  margin: 0 auto;

  :deep .docx-wrapper {
    background: white;
    padding: 10px 0;
  }

  :deep .docx-wrapper > section.docx {
    margin-bottom: 0;
  }
}
</style>
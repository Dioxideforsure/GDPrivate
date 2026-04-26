<script setup>
import * as xlsx from "xlsx"

import {getCurrentInstance, onMounted, ref} from "vue";

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String,
  }
});

const excelContent = ref();
const initExcel = async () => {
  let result = await proxy.Request({
    url: props.url,
    responseType: "arraybuffer"
  });
  if (!result) {
    return;
  }
  let workbook = xlsx.read(new Uint8Array(result), {type: "array"});
  var worksheet = workbook.Sheets[workbook.SheetNames[0]];
  excelContent.value = xlsx.utils.sheet_to_html(worksheet);

};

onMounted(() => {
  initExcel();
})

</script>

<template>
  <div v-html="excelContent" class="table-info"></div>
</template>

<style scoped lang="scss">
.table-info {
  width: 100%;
  padding: 10px;

  :deep table {
    width: 100%;
    border-collapse: collapse;

    td {
      border: 1px solid #ddd;
      border-collapse: collapse;
      padding: 5px;
      height: 30px;
      min-width: 50px;
    }
  }
}
</style>
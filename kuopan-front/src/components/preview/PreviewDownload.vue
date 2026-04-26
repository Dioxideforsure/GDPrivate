<script setup>
import APlayer from "APlayer";
import "APlayer/dist/APlayer.min.css"
import {getCurrentInstance, onMounted, onUnmounted, ref} from "vue";
import Icon from "@/components/Icon.vue";

const {proxy} = getCurrentInstance();

const props = defineProps({
  createDownloadUrl: {
    type: String,
  },
  downloadUrl: {
    type: String,
  },
  fileInfo: {
    type: Object,
  }
});

const download = async () => {
  let result = await proxy.Request({
    url: props.createDownloadUrl,
  })
  if (!result) {
    return;
  }
  window.location.href = props.downloadUrl + "/" + result.data
}


</script>

<template>
  <div class="others">
    <div class="body-content">
      <Icon :iconName="fileInfo.fileType == 0 ? 'zip' : 'others'"
            :width="80"></Icon>

      <div class="file-name">{{ fileInfo.fileName }}</div>
      <div class="tips">该类型的文件暂不支持预览，请下载后查看</div>
      <div class="download-btn">
        <el-button type="primary" @click="download"
        >点击下载{{ proxy.Utils.size2Str(fileInfo.fileSize) }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.others {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;

  .body-content {
    text-align: center;

    .file-name {
      font-weight: bold;
    }

    .tips {
      color: #999898;
      margin-top: 5px;
      font-size: 13px;
    }

    .download-btn {
      margin-top: 20px;
    }
  }
}
</style>
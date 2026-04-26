<script setup>
import {ref, getCurrentInstance} from "vue";
import SparkMD5 from "spark-md5";

const {proxy} = getCurrentInstance()
const api = {
  upload: "/file/uploadFile",
};
const chunkSize = 1024 * 1024 * 5;
const fileList = ref([])
const delList = ref([])
const STATUS = {
  emptyfile: {
    value: "emptyfile",
    desc: "文件为空",
    color: "#F75000",
    icon: "close",
  },
  fail: {
    value: "fail",
    desc: "上传失败",
    color: "#F75000",
    icon: "close",
  },
  init: {
    value: "init",
    desc: "解析中",
    color: "#e6a23c",
    icon: "clock",
  },
  uploading: {
    value: "uploading",
    desc: "上传中",
    color: "#409eff",
    icon: "upload",
  },
  upload_finish: {
    value: "upload_finish",
    desc: "上传完成",
    color: "#67c23a",
    icon: "ok",
  },
  upload_seconds: {
    value: "upload_seconds",
    desc: "秒传",
    color: "#67c23a",
    icon: "ok",
  },
}

const addFile = async (file, filePid) => {
  const fileItem = {
    file: file,
    uid: file.uid,
    md5Progress: 0,
    md5: null,
    fileName: file.name,
    status: STATUS.init.value,
    uploadSize: 0,
    totalSize: file.size,
    uploadProgress: 0,
    pause: false,
    chunkIndex: 0,
    filePid: filePid,
    errorMsg: null
  }
  fileList.value.unshift(fileItem);
  if (fileItem.totalSize == 0) {
    fileItem.status = STATUS.emptyfile.value;
    return;
  }
  let md5FileUid = await computeMD5(fileItem);
  if (md5FileUid == null) {
    return;
  }
  uploadFile(md5FileUid)
};


defineExpose({addFile})

// compute md5 file
const computeMD5 = (fileItem) => {
  let file = fileItem.file;
  let blogSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;

  let chunks = Math.ceil(file.size / chunkSize)
  let currentChunk = 0;
  let spark = new SparkMD5.ArrayBuffer();
  let fileReader = new FileReader();

  let loadNext = () => {
    let start = currentChunk * chunkSize;
    let end = start + chunkSize >= file.size ? file.size : start + chunkSize;
    fileReader.readAsArrayBuffer(blogSlice.call(file, start, end));
  };
  loadNext();


  return new Promise((resolve, reject) => {
    let resultFile = getFileByUid(file.uid);
    fileReader.onload = (e) => {
      spark.append(e.target.result);
      currentChunk++;
      if (currentChunk < chunks) {
        let percent = Math.floor(currentChunk * 100 / chunks );
        resultFile.md5Progress = percent;
        loadNext();
      } else {
        let md5 = spark.end();
        spark.destroy();
        resultFile.md5Progress = 100
        resultFile.status = STATUS.uploading.value;
        resultFile.md5 = md5;
        resolve(fileItem.uid);
      }
    };
    fileReader.onerror = () => {
      resultFile.md5Progress = -1;
      resultFile.status = STATUS.fail.value;
      resolve(fileItem.uid);
    }
  }).catch((error) => {
    console.error(error);
    return null;
  })
}

// get file by uid
const getFileByUid = (uid) => {
  let file = fileList.value.find(item => {
    return item.file.uid === uid;
  })
  return file;
};

const emit = defineEmits(["uploadCallback"])

// upload file
const uploadFile = async (uid, chunkIndex) => {
  chunkIndex = chunkIndex ? chunkIndex : 0;
  // upload in slices
  let currentFile = getFileByUid(uid);
  const file = currentFile.file;
  const fileSize = currentFile.totalSize;
  const chunks = Math.ceil(file.size / chunkSize);
  for (let i = chunkIndex; i < chunks; i++) {
    let delIndex = delList.value.indexOf(uid);
    if (delIndex != -1) {
      delList.value.splice(delIndex, 1);
      break;
    }
    currentFile = getFileByUid(uid);
    if (currentFile.pause) {
      break;
    }
    let start = i * chunkSize;
    let end = start + chunkSize >= fileSize ? fileSize : start + chunkSize;
    let chunkFile = file.slice(start, end);
    let updateResult = await proxy.Request({
      url: api.upload,
      showLoading: false,
      dataFile: "file",
      params: {
        file: chunkFile,
        fileName: currentFile.fileName,
        fileMd5: currentFile.md5,
        chunkIndex: i,
        chunks: chunks,
        fileId: currentFile.fileId,
        filePid: currentFile.filePid,
      },
      showError: false,
      errorCallback: (errorMsg) => {
        currentFile.status = STATUS.fail.value;
        currentFile.errorMsg = errorMsg;
      },
      uploadProgressCallback:(event) => {
        let loaded = event.loaded;
        if (loaded > fileSize) {
          loaded = fileSize;
        }
        currentFile.uploadSize = i * chunkSize + loaded;
        currentFile.uploadProgress = Math.floor(currentFile.uploadSize / fileSize * 100)
      }
    });

    if (updateResult == null) {
      break;
    }
    currentFile.fileId = updateResult.data.fileId;
    currentFile.status = STATUS[updateResult.data.status].value;
    currentFile.chunkIndex = i;
    if (updateResult.data.status == STATUS.upload_seconds.value || updateResult.data.status == STATUS.upload_finish.value) {
      currentFile.uploadProgress = 100;
      emit("uploadCallback");
      break;
    }
  }
};

</script>

<template>
  <div class="uploader-panel">
    <div class="uploader-title">
      <span>上传任务</span>
      <span class="tips">（仅展示本次上传任务）</span>
    </div>

    <div class="file-list">
      <div v-for="(item,index) in fileList" class="file-item">
        <div class="upload-panel">
          <div class="file-name">{{ item.fileName }}</div>
          <div class="progress">
            <el-progress :percentage="item.uploadProgress"
                         v-if="item.status == STATUS.uploading.value
                         || item.status == STATUS.upload_seconds.value || item.status == STATUS.upload_finish.value">

            </el-progress>
          </div>
          <div class="upload-status">
            <span :class="['iconfont', 'icon-' + STATUS[item.status].icon]"
                  :style="{ color: STATUS[item.status].color} ">
            </span>
            <span class="status" :style="{ color: STATUS[item.status].color}">
              {{ item.status == "fail" ? item.errorMsg : STATUS[item.status].desc }}
            </span>

            <span class="upload-info" v-if="item.status == STATUS.uploading.value">
              {{ proxy.Utils.size2Str(item.uploadSize) }} / {{ proxy.Utils.size2Str(item.totalSize) }}
            </span>
          </div>
        </div>
        <div class="op">
          <el-progress
              type="circle"
              :width="50"
              :percentage="item.md5Progress"
              :stroke-width="3"
              v-if="item.status == STATUS.init.value"></el-progress>

          <div class="op-btn">
            <span v-if="item.status == STATUS.uploading.value">
              <Icon :width="28" class="btn-item" iconName="upload" v-if="item.pause" title="上传"
                    @click="startUpload(item.uid)"></Icon>
              <Icon
                  v-else
                  :width="28" class="btn-item" iconName="pause" title="暂停"
                  @click="pauseUpload(item.uid)"></Icon>
              <Icon
                  :width="28" class="del btn-item" iconName="del"
                  v-if="item.status != STATUS.init.value
                  && item.status != STATUS.upload_finish.value
                  && item.status != STATUS.upload_seconds.value" title="删除"
                  @click="delUpload(item.uid, index)"></Icon>
            </span>
            <Icon
                :width="28" class="clean btn-item" iconName="clean"
                v-if="item.status == STATUS.upload_finish.value|| item.status == STATUS.upload_seconds.value"
                title="清除"
                @click="delUpload(item.uid, index)"></Icon>
          </div>
        </div>
      </div>
    </div>
    <div v-if="fileList.length == 0">
      <NoData msg="暂无上传任务"></NoData>
    </div>
  </div>

</template>

<style scoped lang="scss">
.uploader-panel {
  .uploader-title {
    border-bottom: 1px solid #ddd;
    line-height: 40px;
    padding: 0px 10px;
    font-size: 15px;

    .tips {
      font-size: 13px;
      color: rgb(169, 169, 169);
    }
  }

  .file-list {
    overflow: auto;
    padding: 10px 0px;
    min-height: calc(100vh / 2);
    max-height: calc(100vh - 120px);

    .file-item {
      position: relative;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 3px 10px;
      background-color: #fff;
      border-bottom: 1px solid #ddd;
    }

    .file-item:nth-child(even) {
      background-color: #fcf8f4;
    }

    .upload-panel {
      flex: 1;

      .file-name {
        color: rgb(64, 62, 62);
        margin-left: 5px;
        font-size: 12px;
        color: rgb(112, 111, 111);
      }

      .upload-status {
        display: flex;
        align-items: center;
        margin-top: 5px;

        .iconfont {
          margin-right: 3px;
        }

        .status {
          color: red;
          font-size: 13px;
        }

        .upload-info {
          margin-left: 5px;
          font-size: 12px;
          color: rgb(112, 111, 111);
        }
      }

      .progress {
        height: 10px;
      }
    }
  }

  .op {
    width: 100px;
    display: flex;
    align-items: center;
    justify-content: flex-end;


    .op-btn {
      .btn-item {
        cursor: pointer;
      }

      .del,
      .clean {
        margin-left: 5px;
      }
    }
  }
}
</style>
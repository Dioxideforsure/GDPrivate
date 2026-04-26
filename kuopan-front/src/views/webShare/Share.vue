<script setup>
import {getCurrentInstance, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import Navigation from "@/components/Navigation.vue";
import Table from "@/components/Table.vue";
import FolderSelect from "@/components/FolderSelect.vue";
import Preview from "@/components/preview/Preview.vue";

const {proxy} = getCurrentInstance();
const route = useRoute();
const router = useRouter();

const api = {
  getShareLoginInfo: "/showShare/getShareLoginInfo",
  loadFileList: "/showShare/loadFileList",
  createDownloadUrl: "/showShare/createDownloadUrl",
  download: "/api/showShare/download",
  cancelShare: "/share/cancelShare",
  saveShare: "/showShare/saveShare",
};

const shareId = route.params.shareId;
const shareInfo = ref({});
const getShareInfo = async () => {
  let result = await proxy.Request({
    url: api.getShareLoginInfo,
    showLoading: false,
    params: {
      shareId
    },
  });
  if (!result) {
    return;
  }
  if (result.data == null) {
    await router.push(`/shareCheck/${shareId}`);
  }
  shareInfo.value = result.data
}

getShareInfo();


const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName",
  },
  {
    label: "修改时间",
    prop: "lastUpdateTime",
    width: 200,
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width: 200,
  },
];

const tableData = ref({});
const tableOptions = {
  exHeight: 80,
  selectType: "checkbox"
};

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    shareId: shareId,
    filePid: currentFolder.value.fileId,
  };
  let result = await proxy.Request({
    url: api.loadFileList,
    params: params
  });
  if (!result) {
    return;
  }
  tableData.value = result.data;
};

// Multi-select
const selectFileIdList = ref([]);
const rowSelected = (rows) => {
  selectFileIdList.value = [];
  rows.forEach(item => {
    selectFileIdList.value.push(item.fileId)
  })
};

// Show operation buttons
const showOp = (row) => {
  tableData.value.list.forEach(element => {
    element.showOp = false;
  });
  row.showOp = true;
}

const cancelShowOp = (row) => {
  row.showOp = false
}

const currentFolder = ref({fileId: 0});
const naviChange = (data) => {
  const {curFolder} = data;
  currentFolder.value = curFolder;
  loadDataList();
}

// Preview
const previewRef = ref();
const navigationRef = ref();

const preview = (data) => {
  if (data.folderType == 1) {
    navigationRef.value.openFolder(data);
    return;
  }
  data.shareId = shareId;
  previewRef.value.showPreview(data, 2);
}

// Download files
const download = async (row) => {
  let result = await proxy.Request({
    url: api.createDownloadUrl + "/" + shareId + "/" + row.fileId,
  })
  if (!result) {
    return;
  }

  window.location.href = api.download + "/" + result.data
}

// Save to my cloud
const folderSelectRef = ref();
const save2MyPanFileIdArray = ref([]);
const save2MyPan = () => {
  if (selectFileIdList.value.length == 0) {
    return;
  }
  if (!proxy.VueCookies.get("userInfo")) {
    router.push("/login?redirectUrl=" + route.path);
    return;
  }
  save2MyPanFileIdArray.value = selectFileIdList.value;
  folderSelectRef.value.showFolderDialog();
}

const save2MyPanOnce = (row) => {
  if (!proxy.VueCookies.get("userInfo")) {
    router.push("/login?redirectUrl=" + route.path);
    return;
  }
  save2MyPanFileIdArray.value = [row.fileId];
  folderSelectRef.value.showFolderDialog();
}

const save2MyPanDone = async (folderId) => {
  let result = await proxy.Request({
    url: api.saveShare,
    params: {
      shareId: shareId,
      shareFileIds: save2MyPanFileIdArray.value.join(","),
      myFolderId: folderId,

    },
  })
  if (!result) {
    return;
  }
  await loadDataList();
  proxy.Message.success("保存成功");
  folderSelectRef.value.close();
}

const cancelShare = () => {
  proxy.Confirm(`你确定要取消分享吗`,
      async () => {
        let result = await proxy.Request({
          url: api.cancelShare,
          params: {
            shareIds: shareId,
          },
        })
        if (!result) {
          return;
        }
        ;
        proxy.Message.success("取消分享成功");
        router.push("/");
      })
}

const jump = () => {
  router.push("/")
}
</script>

<template>
  <div class="share">
    <div class="header">
      <div class="header-content">
        <div class="logo" @click="jump">
          <span class="iconfont icon-pan"></span>
          <span class="name">阔盘</span>
        </div>
      </div>
    </div>
  </div>

  <div class="share-body">
    <template v-if="Object.keys(shareInfo).length == 0">
      <div class="loading" v-loading="Object.keys(shareInfo).length == 0"></div>
    </template>
    <template v-else>
      <div class="share-panel">
        <div class="share-user-info">
          <div class="share-info">
            <div class="user-info">
              <span class="nick-name">{{ shareInfo.userName }}</span>
              <span class="share-time">分享于：{{ shareInfo.shareTime }}</span>
            </div>
            <div class="file-name">分享文件：{{ shareInfo.fileName }}</div>
          </div>
        </div>
        <div class="share-op-btn">
          <el-button type="primary" @click="cancelShare" v-if="shareInfo.currentUser">
            <span class="iconfont icon-cancel">
            取消分享</span>
          </el-button>
          <el-button v-else type="primary" @click="save2MyPan" :disabled="selectFileIdList.length==0">
            <span class="iconfont icon-import">
            </span>保存到我的云盘
          </el-button>
        </div>
      </div>
      <Navigation ref="navigationRef" @naviChange="naviChange" :shareId="shareId"></Navigation>

      <div class="file-list">
        <Table ref="dataTableRef"
               :columns="columns"
               :showPagination="true"
               :dataSource="tableData"
               :fetch="loadDataList"
               :initFetch="false"
               :options="tableOptions"
               @rowSelected="rowSelected">
          <template #fileName="{index, row}">
            <div class="file-item" @mouseenter="showOp(row)" @mouseleave="cancelShowOp(row)">

              <template v-if="(row.fileType == 3 || row.fileType == 1) && row.status == 2">
                <Icon :cover="row.fileCover" :width="32"></Icon>
              </template>

              <template v-else>
                <Icon v-if="row.folderType == 0" :fileType="row.fileType"></Icon>
                <Icon v-if="row.folderType == 1" :fileType="0"></Icon>
              </template>

              <span class="file-name" :title="row.fileName">
              <span @click="preview(row)">{{ row.fileName }}</span>
            </span>

              <span class="op">
                <span class="iconfont icon-download" v-if="row.folderType == 0" @click="download(row)">下载</span>
                <span class="iconfont icon-import" @click="save2MyPanOnce(row)"
                      v-if="row.showOp && !shareInfo.currentUser">保存到我的网盘</span>
            </span>
            </div>
          </template>
          <template #fileSize="{index, row}">
            <span v-if="row.fileSize">{{ proxy.Utils.size2Str(row.fileSize) }}</span>
          </template>
        </Table>
      </div>
    </template>
    <!--    Folder Select    -->
    <FolderSelect ref="folderSelectRef" @folderSelect="save2MyPanDone"></FolderSelect>
    <Preview ref="previewRef"></Preview>
  </div>
</template>

<style scoped lang="scss">
@import "@/assets/file.list.scss";

.header {
  width: 100%;
  position: fixed;
  background: #0c95f7;
  height: 50px;

  .header-content {
    width: 70%;
    margin: 0px auto;
    color: #fff;
    line-height: 50px;

    .logo {
      display: flex;
      align-items: center;
      cursor: pointer;

      .icon-pan {
        font-size: 40px;
      }

      .name {
        font-weight: bold;
        margin-left: 5px;
        font-size: 25px;
      }
    }
  }
}

.share-body {
  width: 70%;
  margin: 0px auto;
  padding-top: 50px;

  .loading {
    height: calc(100vh / 2);
    width: 100%;
  }

  .share-panel {
    margin-top: 20px;
    display: flex;
    justify-content: space-around;
    border-bottom: 1px solid #ddd;
    padding-bottom: 10px;

    .share-user-info {
      flex: 1;
      display: flex;
      align-items: center;

      .avatar {
        margin-right: 5px;
      }

      .share-info {
        .user-info {
          display: flex;
          align-items: center;

          .nick-name {
            font-size: 15px;
          }

          .share-time {
            margin-left: 20px;
            font-size: 12px;
          }
        }

        .file-name {
          margin-top: 10px;
          font-size: 12px;
        }
      }
    }
  }
}

.file-list {
  margin-top: 10px;

  .file-item {
    .op {
      width: 170px;
    }
  }
}
</style>
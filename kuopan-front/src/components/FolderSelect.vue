<script setup>

import Dialog from "@/components/Dialog.vue";
import {getCurrentInstance, ref} from "vue";
import Icon from "@/components/Icon.vue";

const {proxy} = getCurrentInstance();

const api = {
  loadAllFolder:"/file/loadAllFolder"
}

const dialogConfig = ref({
  show: false,
  title: "移动到",
  buttons: [
    {
      type: "primary",
      click :(e) => {
        folderSelectConfirm();
      },
      text: "移动到此",
    },

  ]
})

// parent Id
const filePid = ref("0");

const folderList = ref([]);
const currentFileIds = ref({});
const currentFolder = ref({});
const navigationRef = ref();


const loadAllFolder = async () => {
  let result = await proxy.Request({
    url:api.loadAllFolder,
    params:{
      filePid: filePid.value,
      currentFileIds: currentFileIds.value,
    },
  });
  if (!result) {
    return;
  }
  folderList.value = result.data;
}

const close = () => {
  dialogConfig.value.show = false;
}

const showFolderDialog = (currentFolder) => {
  dialogConfig.value.show = true;
  currentFileIds.value = currentFolder
  loadAllFolder();
};

defineExpose({
  showFolderDialog,
  close
})

// Select folder
const selectFolder = (data) => {
  navigationRef.value.openFolder(data);
}

// confirm selected folder
const emit = defineEmits(["folderSelect"])
const folderSelectConfirm = () => {
    emit("folderSelect", filePid.value)
}

// navigation folder call back
const naviChange = (data) => {
  const {curFolder} = data;
  currentFolder.value = curFolder;
  filePid.value = curFolder.fileId;
  loadAllFolder();
}
</script>

<template>
<div>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="400px"
    :showCancel="false"
    @close="dialogConfig.show = false">
    <div class="navigation-panel">
      <Navigation ref="navigationRef" @naviChange="naviChange" :watchPath="false"></Navigation>
    </div>
    <div class="folder-list" v-if="folderList.length > 0">
      <div class="folder-item" v-for="item in folderList" @click="selectFolder(item)">
        <Icon :fileType="0"></Icon>
        <span class="file-name">{{ item.fileName }}</span>
      </div>
    </div>
    <div v-else class="tips">
      移动到 <span>{{ currentFolder.fileName }} </span>
    </div>
  </Dialog>
</div>
</template>

<style scoped lang="scss">
.navigation-panel {
  padding-left: 10px;
  background: #f1f1f1;
}

.folder-list {
  .folder-item {
    cursor: pointer;
    display: flex;
    align-items: center;
    padding: 10px;

    .file-name {
      display: inline-block;
      margin-left: 10px;
    }

    &:hover {
      background: #f8f8f8;
    }
  }

  max-height: calc(100vh - 200px);
  min-height: 200px;
}

.tips {
  text-align: center;
  line-height: 200px;

  span {
    color: #06a7ff;
  }
}

</style>
export default {
    size2Str: (limit) => {
        var size = "";
        if (limit < 0.1 * 1024) {
            size = limit.toFixed(2) + "B" // Show bytes if smaller than 0.1 KB
        } else if (limit < 0.1 * 1024 * 1024) {
            size = (limit / 1024).toFixed(2) + "KB" // Show kilobytes if smaller than 0.1 MB
        }else if (limit < 0.1 * 1024 * 1024 * 1024) {
            size = (limit / 1024 / 1024).toFixed(2) + "MB"; // Show megabytes if smaller than 0.1 GB
        } else {
            size = (limit / 1024 / 1024 / 1024 ).toFixed(2) + "GB"; // Show gigabytes when bigger than 0.1GB
        }
        var sizeStr = size + ""; // To string
        var index = sizeStr.indexOf(".");
        var dou = sizeStr.substr(index + 1, 2); // Get the last 2 digits of the point
        if (dou == "00") {
            return sizeStr.substring(0, index) + sizeStr.substr(index + 3, 2);
        } // Delete if the digits are "00"
        return size;
    }
}
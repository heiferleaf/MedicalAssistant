/**
 * 统一拍照工具
 *
 * 策略（按优先级）：
 *  1. App 端：优先加载原生插件 yun-camerax-module，加载失败则降级为 uni.chooseImage
 *  2. H5 端：使用 uni.chooseImage
 */

function takePhotoByBuiltIn(options) {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sourceType: ['camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        console.log("拍照成功，路径:", tempFilePath);

        uni.setStorageSync("last_scan_image", tempFilePath);

        if (options.autoJump !== false) {
          uni.navigateTo({
            url: "/pages/ai/Assistant?from=scan",
            success: () => {
              uni.hideLoading();
            },
          });
        }

        resolve(tempFilePath);
      },
      fail: (err) => {
        console.warn("拍照取消或失败:", err);
        reject(err);
      }
    });
  });
}

/**
 * 通用拍照并跳转识别逻辑
 * @param {Object} options 自定义配置
 * @param {string} options.text 扫描框提示文字
 * @param {boolean} options.autoJump 是否自动跳转到 AI 助手页，默认为 true
 */
export const takeDrugPhoto = (options = {}) => {
  // #ifdef APP-PLUS
  try {
    const cameraModule = uni.requireNativePlugin("yun-camerax-module");
    if (cameraModule && typeof cameraModule.takePhoto === 'function') {
      return new Promise((resolve, reject) => {
        cameraModule.takePhoto(
          {
            type: options.type || 0,
            imageIndex: options.imageIndex || 1,
            text: options.text || "请正对药盒拍摄\n确保药品信息清晰可见",
            landscape: options.landscape || false,
          },
          (res) => {
            console.log("原生插件返回原始数据:", res);

            if (res && res.file) {
              const finalPath = "file://" + res.file;

              uni.setStorageSync("last_scan_image", finalPath);

              if (options.autoJump !== false) {
                uni.navigateTo({
                  url: "/pages/ai/Assistant?from=scan",
                  success: () => {
                    uni.hideLoading();
                  },
                });
              }

              resolve(finalPath);
            } else {
              console.warn("拍照未完成或取消");
              reject(new Error("User cancelled or failed"));
            }
          }
        );
      });
    }
  } catch (e) {
    console.warn("原生拍照插件不可用，降级为内置拍照:", e);
  }
  // #endif

  // H5 或原生插件不可用时，使用内置拍照
  return takePhotoByBuiltIn(options);
};

export default {
  takeDrugPhoto
};
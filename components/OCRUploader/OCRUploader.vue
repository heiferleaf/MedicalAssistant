<template>
	<!-- 空组件，仅作为 renderjs 的载体 -->
	<view ref="ocrView"></view>
</template>

<script>
// 普通 script 块，用于暴露方法给父组件
export default {
	named: 'OCRUploader',
	methods: {
		// 调用 renderjs 的 OCR 方法
		callOCR(ocrData) {
			console.log('[OCRUploader] callOCR 被调用，ocrData:', ocrData ? '有数据' : '空')
			// #ifdef APP-PLUS
			// 通过 plus.webview 调用 renderjs 的全局方法
			const currentWebview = plus.webview.currentWebview()
			console.log('[OCRUploader] 获取 webview:', currentWebview ? '成功' : '失败')
			// 使用 evalJS 执行 renderjs 中的代码
			const jsCode = `renderjsTriggerOCR(${JSON.stringify(ocrData)})`
			console.log('[OCRUploader] 准备执行 JS:', jsCode.substring(0, 100))
			currentWebview.evalJS(jsCode)
			console.log('[OCRUploader] evalJS 执行完成')
			// #endif
		}
	}
}
</script>

<script module="ocr" lang="renderjs">
// 暴露全局方法供普通 script 调用
function renderjsTriggerOCR(ocrData) {
	console.log('renderjs: renderjsTriggerOCR 被调用')
	triggerOCR(ocrData)
}

export default {
	methods: {
		// 手动触发 OCR 处理
		triggerOCR(ocrData) {
			console.log('renderjs: triggerOCR 被调用')
			console.log('renderjs: ocrData =', JSON.stringify(ocrData))
			
			if (!ocrData || !ocrData.base64Data || !ocrData.ocrUrl) {
				console.error('renderjs: OCR 参数不完整', ocrData)
				return
			}
			
			console.log('renderjs: 开始 OCR 识别')
			console.log('renderjs: Base64 长度 =', ocrData.base64Data.length)
			
			// 将 Base64 转换为 Blob
			const blob = this.base64ToBlob(ocrData.base64Data)
			console.log('renderjs: Blob 创建成功，大小:', blob.size)
			
			// 创建 FormData
			const formData = new FormData()
			formData.append('file', blob, 'drug.jpg')
			console.log('renderjs: FormData 创建成功')
			
			// 发送请求
			console.log('renderjs: 开始发送请求到', ocrData.ocrUrl)
			fetch(ocrData.ocrUrl, {
				method: 'POST',
				body: formData
			})
			.then(response => {
				console.log('renderjs: OCR 响应状态码:', response.status)
				return response.json()
			})
			.then(data => {
				console.log('renderjs: OCR 识别成功', data)
				this.$ownerInstance.callMethod('onSuccess', data)
			})
			.catch(err => {
				console.error('renderjs: OCR 识别失败', err)
				this.$ownerInstance.callMethod('onError', err)
			})
		},
		// Base64 转 Blob
		base64ToBlob(base64Data, type = 'image/jpeg') {
			// 去掉前缀
			let cleanBase64 = base64Data
			if (cleanBase64.startsWith('data:')) {
				cleanBase64 = cleanBase64.split(',')[1]
			}
			
			// 转换为字节数组
			const byteCharacters = atob(cleanBase64)
			const byteNumbers = new Array(byteCharacters.length)
			for (let i = 0; i < byteCharacters.length; i++) {
				byteNumbers[i] = byteCharacters.charCodeAt(i)
			}
			const byteArray = new Uint8Array(byteNumbers)
			
			// 创建 Blob
			return new Blob([byteArray], { type: type })
		}
	}
}
</script>

<style>
</style>

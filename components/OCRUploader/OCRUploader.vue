<template>
	<!-- 空组件，仅作为 renderjs 的载体 -->
	<view 
		:ocrData="ocrData" 
		:change:ocrData="ocr.triggerOCR"
	></view>
</template>

<script>
// 普通 script 块，用于暴露方法给父组件
export default {
	named: 'OCRUploader',
	data() {
		return {
			ocrData: null
		}
	},
	methods: {
		// 调用 renderjs 的 OCR 方法
		callOCR(ocrData) {
			console.log('[OCRUploader] callOCR 被调用，ocrData:', ocrData ? '有数据' : '空')
			// #ifdef APP-PLUS
			// 通过修改数据触发 renderjs 的 :change
			this.ocrData = ocrData
			// #endif
		},
		// renderjs 调用此方法通知 OCR 成功
		onSuccess(data) {
			console.log('[OCRUploader] onSuccess 被调用')
			// 触发父组件的方法
			this.$emit('ocr-success', data)
		},
		// renderjs 调用此方法通知 OCR 失败
		onError(error) {
			console.log('[OCRUploader] onError 被调用', error)
			this.$emit('ocr-error', error)
		}
	}
}
</script>

<script module="ocr" lang="renderjs">
export default {
	methods: {
		// 通过 :change 触发
		triggerOCR(newVal, oldVal, ownerInstance, instance) {
			if (!newVal) {
				return
			}
			
			console.log('renderjs: triggerOCR 被调用')
			console.log('renderjs: ocrData =', JSON.stringify(newVal).substring(0, 100) + '...')
			
			if (!newVal || !newVal.base64Data || !newVal.ocrUrl) {
				console.error('renderjs: OCR 参数不完整', newVal)
				return
			}
			
			console.log('renderjs: 开始 OCR 识别')
			console.log('renderjs: Base64 长度 =', newVal.base64Data.length)
			
			// 将 Base64 转换为 Blob
			const blob = base64ToBlob(newVal.base64Data)
			console.log('renderjs: Blob 创建成功，大小:', blob.size)
			
			// 创建 FormData
			const formData = new FormData()
			formData.append('file', blob, 'drug.jpg')
			console.log('renderjs: FormData 创建成功')
			
			// 发送请求
			console.log('renderjs: 开始发送请求到', newVal.ocrUrl)
			fetch(newVal.ocrUrl, {
				method: 'POST',
				body: formData
			})
			.then(response => {
				console.log('renderjs: OCR 响应状态码:', response.status)
				return response.json()
			})
			.then(data => {
				console.log('renderjs: OCR 识别成功', Object.keys(data))
				// 使用 ownerInstance.callMethod 调用父组件方法
				if (ownerInstance && ownerInstance.callMethod) {
					ownerInstance.callMethod('onSuccess', data)
					console.log('renderjs: 已调用 onSuccess')
				} else {
					console.error('renderjs: ownerInstance 或 callMethod 不存在')
				}
			})
			.catch(err => {
				console.error('renderjs: OCR 识别失败', err)
				if (ownerInstance && ownerInstance.callMethod) {
					ownerInstance.callMethod('onError', err)
				}
			})
		}
	}
}

// Base64 转 Blob 工具函数（放在外部，不依赖 this）
function base64ToBlob(base64Data, type = 'image/jpeg') {
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
</script>

<style>
</style>

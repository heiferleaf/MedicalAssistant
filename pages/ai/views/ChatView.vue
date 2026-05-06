<template>
	<view class="chat-view">
		<scroll-view 
			scroll-y 
			class="chat-main" 
			:scroll-into-view="scrollToMsgId" 
			scroll-with-animation
			@scrolltolower="loadMore"
		>
			<view class="chat-content">
				<!-- 消息列表 -->
				<view
					v-for="msg in messages"
					:key="msg.id"
					:id="'msg-' + msg.id"
					class="message-wrapper"
				>
					<!-- 调试：输出消息数据 -->
					<!-- <text>消息类型：{{ msg.role }}, 图片：{{ !!msg.imagePath }}</text> -->
					<ChatMessage
						:messageId="msg.id"
						:role="msg.role"
						:type="msg.type"
						:content="msg.content"
						:image="msg.imagePath || msg.image"
						:actionType="msg.actionType"
						:actionData="msg.actionData"
						@action-confirm="handleActionConfirm"
						@action-edit="handleActionEdit"
						@action-cancel="handleActionCancel"
					>
						<!-- 插槽：用于扩展特殊消息类型 -->
						<slot :name="'message-' + msg.id" :msg="msg"></slot>
					</ChatMessage>
				</view>
				
				<!-- 加载状态 -->
				<view v-if="loading" class="loading-message">
					<ChatMessage role="loading" />
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import ChatMessage from '../components/ChatMessage.vue';

export default {
	name: 'ChatView',
	components: {
		ChatMessage
	},
	props: {
		messages: {
			type: Array,
			default: () => []
		},
		scrollToMsgId: {
			type: String,
			default: ''
		},
		loading: {
			type: Boolean,
			default: false
		},
		statusText: {
			type: String,
			default: '思考中...'
		}
	},
	methods: {
		loadMore() {
			this.$emit('load-more');
		},
		scrollToBottom() {
			if (this.messages.length > 0) {
				this.$emit('scroll-to', 'msg-' + (this.messages.length - 1));
			}
		},
		handleActionConfirm(event) {
			this.$emit('action-confirm', event);
		},
		handleActionEdit(event) {
			this.$emit('action-edit', event);
		},
		handleActionCancel(event) {
			this.$emit('action-cancel', event);
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-view {
	flex: 1;
	height: 0;
	overflow: hidden;
	background-color: transparent; // 透明背景，显示底层 logo
	
	@media (prefers-color-scheme: dark) {
		background-color: transparent;
	}
}

.chat-main {
	height: 100%;
	
	.chat-content {
		padding: 30rpx;
		padding-bottom: 60rpx;
		
		.message-wrapper {
			width: 100%;
			display: flex;
			flex-direction: column;
			align-items: flex-end;  // 所有内容靠右对齐
			margin-bottom: 40rpx;  // 消息间距
			
			&:last-child {
				margin-bottom: 0;
			}
		}
		
		.loading-message {
			width: 100%;
			display: flex;
			justify-content: flex-start;  // loading 靠左
			margin-top: 20rpx;
		}
	}
}
</style>

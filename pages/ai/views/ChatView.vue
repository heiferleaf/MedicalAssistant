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
				<view
					v-for="msg in messages"
					:key="msg.id"
					:id="'msg-' + msg.id"
				>
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
	}
}
</style>

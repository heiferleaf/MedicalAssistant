package com.whu.medicalbackend.agent.langchain4j.tools.rag;

import com.whu.medicalbackend.rag.RagRequest;
import com.whu.medicalbackend.rag.RagResponse;
import com.whu.medicalbackend.rag.RagService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RagTool {

    private static final Logger logger = LoggerFactory.getLogger(RagTool.class);

    @Autowired
    private RagService ragService;

    @Tool("Query medical knowledge base using RAG (Retrieval-Augmented Generation). " +
          "This tool can answer medical questions by searching through medical literature and knowledge base. " +
          "Use this when users ask medical questions, symptoms, treatments, or need professional medical information.")
    public String queryMedicalKnowledge(String question) {
        logger.info("使用RAG工具查询医学知识: {}", question);
        
        try {
            RagRequest request = new RagRequest(question);
            RagResponse response = ragService.queryRag(request);
            
            if (response.isSuccess()) {
                logger.info("RAG查询成功");
                return response.getAnswer();
            } else {
                logger.error("RAG查询失败: {}", response.getError());
                return "抱歉，医学知识库查询失败：" + response.getError();
            }
        } catch (Exception e) {
            logger.error("RAG工具执行异常", e);
            return "抱歉，医学知识库暂时不可用，请稍后再试。";
        }
    }
}
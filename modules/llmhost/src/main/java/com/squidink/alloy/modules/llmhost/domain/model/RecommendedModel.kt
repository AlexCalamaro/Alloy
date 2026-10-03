package com.squidink.alloy.modules.llmhost.domain.model

/**
 * Curated recommendation for on-device LiteRT models suitable for Googlebook OS compact laptops.
 */
data class RecommendedModel(
    val id: String,
    val name: String,
    val description: String,
    val modelSizeDisplay: String,
    val downloadUrl: String,
    val format: ModelFormat = ModelFormat.fromFileName(downloadUrl),
    val quantization: String,
    val hardwareRecommendation: String,
    val requiresHfToken: Boolean
) {
    companion object {
        /**
         * Curated list of LiteRT-LM and GGUF models verified for Googlebook OS (compact laptops with 16 GB RAM).
         */
        val CURATED_MODELS: List<RecommendedModel> = listOf(
            // --- GGUF Models (Llama.cpp Engine) ---
            RecommendedModel(
                id = "qwen-2.5-7b-instruct-gguf",
                name = "Qwen 2.5 7B Instruct (GGUF)",
                description = "Desktop-class reasoning, coding, and multilingual knowledge. Outstanding general intelligence.",
                modelSizeDisplay = "~4.7 GB",
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf",
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                hardwareRecommendation = "Optimized for ARMv8.2-A i8mm/dotprod. ~5.5 GB RAM footprint.",
                requiresHfToken = false
            ),
            RecommendedModel(
                id = "deepseek-r1-distill-qwen-8b-gguf",
                name = "DeepSeek R1 Distill 8B (GGUF)",
                description = "State-of-the-art step-by-step reasoning, logic, and math on local hardware.",
                modelSizeDisplay = "~5.2 GB",
                downloadUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-8B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-8B-Q4_K_M.gguf",
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                hardwareRecommendation = "Fits comfortably in 16 GB RAM with generous multitasking space.",
                requiresHfToken = false
            ),
            RecommendedModel(
                id = "llama-3.2-3b-instruct-gguf",
                name = "Llama 3.2 3B Instruct (GGUF)",
                description = "Balanced high-speed reasoning with negligible battery drain and fast token generation.",
                modelSizeDisplay = "~2.0 GB",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                hardwareRecommendation = "15-25 tokens/s on ARM NEON. Sub-2.5 GB RAM footprint.",
                requiresHfToken = false
            ),
            RecommendedModel(
                id = "smollm2-1.7b-instruct-gguf",
                name = "SmolLM2 1.7B Instruct (GGUF)",
                description = "Ultra-lightweight on-device assistant. Instantaneous time-to-first-token.",
                modelSizeDisplay = "~1.1 GB",
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                hardwareRecommendation = "Minimal RAM (<1.8 GB). Ideal for continuous background tasks.",
                requiresHfToken = false
            ),
            // --- LiteRT Models (Google AI Edge Engine) ---
            RecommendedModel(
                id = "gemma-2-2b-it",
                name = "Gemma 2 2B IT (LiteRT)",
                description = "Fast, instruction-tuned edge model. " +
                    "Delivers snappy responses with negligible thermal impact and long battery life.",
                modelSizeDisplay = "~1.5 GB",
                downloadUrl = "https://huggingface.co/litert-community/Gemma-2-2B-IT-LiteRT/" +
                    "resolve/main/gemma-2-2b-it-gpu.litertlm",
                format = ModelFormat.LITERT,
                quantization = "int4",
                hardwareRecommendation = "Recommended default: Lowest RAM footprint " +
                    "(~2.5 GB with KV-cache). Excellent for background tools.",
                requiresHfToken = true
            ),
            RecommendedModel(
                id = "gemma-2-9b-it",
                name = "Gemma 2 9B IT (LiteRT)",
                description = "Desktop-class reasoning, code generation, and complex analysis. " +
                    "Exceptional instruction following for local power tools.",
                modelSizeDisplay = "~5.4 GB",
                downloadUrl = "https://huggingface.co/litert-community/Gemma-2-9B-IT-LiteRT/" +
                    "resolve/main/gemma-2-9b-it-gpu.litertlm",
                format = ModelFormat.LITERT,
                quantization = "int4",
                hardwareRecommendation = "Comfortably accommodated within the 16 GB RAM envelope " +
                    "with ample room for desktop multitasking.",
                requiresHfToken = true
            ),
            RecommendedModel(
                id = "phi-3.5-mini-instruct",
                name = "Phi-3.5 Mini Instruct (LiteRT)",
                description = "High-efficiency 3.8B parameter model with impressive math, " +
                    "reasoning, and multi-turn instruction fidelity.",
                modelSizeDisplay = "~2.2 GB",
                downloadUrl = "https://huggingface.co/litert-community/Phi-3.5-mini-instruct-LiteRT/" +
                    "resolve/main/phi-3.5-mini-instruct-gpu.litertlm",
                format = ModelFormat.LITERT,
                quantization = "int4",
                hardwareRecommendation = "Balanced power/speed ratio for 16 GB compact laptops.",
                requiresHfToken = false
            ),
            RecommendedModel(
                id = "llama-3.2-1b-instruct",
                name = "Llama 3.2 1B Instruct (LiteRT)",
                description = "Ultra-compact model designed for swift background automation, " +
                    "classification, and text transformation.",
                modelSizeDisplay = "~1.1 GB",
                downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct-LiteRT/" +
                    "resolve/main/llama-3.2-1b-instruct-gpu.litertlm",
                format = ModelFormat.LITERT,
                quantization = "int4",
                hardwareRecommendation = "Near-instantaneous time-to-first-token with sub-2W compute draw.",
                requiresHfToken = true
            )
        )
    }
}

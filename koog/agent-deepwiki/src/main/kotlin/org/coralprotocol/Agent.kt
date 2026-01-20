package org.coralprotocol

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.executeMultipleTools
import ai.koog.agents.core.dsl.extension.extractToolCalls
import ai.koog.agents.core.dsl.extension.latestTokenUsage
import ai.koog.agents.core.dsl.extension.requestLLMOnlyCallingTools
import ai.koog.agents.core.environment.result
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.coralprotocol.util.*
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

data class DeepWikiSettings(
    val devinApiKey: String
)

@OptIn(ExperimentalUuidApi::class)
fun main() {
    runBlocking {
        val deepWikiSettings = loadAgentSettingsFromEnvironment<DeepWikiSettings>()
        val basicSettings = loadAgentSettingsFromEnvironment<BasicAgentSettings>()
        val executor: PromptExecutor = SingleLLMPromptExecutor(
            OpenRouterLLMClient(
                apiKey = basicSettings.modelApiKey,
                settings = OpenRouterClientSettings()
            )
        )
        val llmModel = findKoogModelByName(basicSettings.modelId)

        println("Connecting to MCP server at ${basicSettings.coralConnectionUrl}")
        val coralMcpClient = getMcpClient(basicSettings.coralConnectionUrl)
        val coralToolRegistry = McpToolRegistryProvider.fromClient(coralMcpClient)

        val wikiMcpClient = getMcpClient("https://mcp.devin.ai/sse") {
            bearerAuth(deepWikiSettings.devinApiKey)
        }
        val wikiToolRegistry = McpToolRegistryProvider.fromClient(wikiMcpClient)

        val toolRegistry = ToolRegistry {
            tools(coralToolRegistry.tools)
            tools(wikiToolRegistry.tools)
        }

        println("Available tools: ${toolRegistry.tools.joinToString { it.name }}")

        val loopAgent = AIAgent.Companion(
            systemPrompt = "", // This gets replaced later
            promptExecutor = executor,
            llmModel = llmModel,
            toolRegistry = toolRegistry,
            strategy = functionalStrategy { _: Nothing? ->
                val maxIterations = basicSettings.maxIterations
                val claimHandler = ClaimHandler(currency = "usd")

                repeat(maxIterations) { i ->
                    try {
                        if (claimHandler.noBudget()) return@functionalStrategy

                        updateSystemResources(coralMcpClient, basicSettings)
                        val response =
                            requestLLMOnlyCallingTools(if (i == 0) buildInitialUserMessage(basicSettings) else basicSettings.followUpUserPrompt)

                        println("Iteration $i LLM response: ${response.content}")
                        val toolsToCall = extractToolCalls(listOf(response))
                        println("Extracted tool calls: ${toolsToCall.joinToString { it.tool }}")
                        val toolResult = executeMultipleTools(toolsToCall)
                        println("Executed tools, got ${toolResult.size} results: ${Json.encodeToString(toolResult.map { it.toMessage() })}")
                        llm.writeSession {
                            appendPrompt {
                                tool {
                                    toolResult.forEach { toolResult -> this@tool.result(toolResult) }
                                }
                            }
                        }

                        // For debugging: save the full prompt messages to a file
                        llm.readSession {
                            val file = File("agent_log.json")
                            file.writeText(Json.encodeToString(prompt.messages))
                        }

                        val tokens = latestTokenUsage()
                        if (tokens > 0) {
                            val toClaim = tokens.toDouble() * USD_PER_TOKEN
                            try {
                                claimHandler.claim(toClaim)
                            } catch (e: Exception) {
                                // If a claim fails, stop to avoid unpaid work when orchestrated
                                e.printStackTrace()
                                return@functionalStrategy
                            }
                        }
                    } catch (e: Exception) {
                        println("Error during agent iteration: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        )

        loopAgent.run(null)
    }
}
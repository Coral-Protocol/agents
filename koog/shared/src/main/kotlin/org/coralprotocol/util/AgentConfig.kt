@file:OptIn(ExperimentalHoplite::class)

package org.coralprotocol.util

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addEnvironmentSource

inline fun <reified T : Any> loadAgentSettingsFromEnvironment(): T {
    val loader = ConfigLoaderBuilder.default()
        .withExplicitSealedTypes("type")
        .addEnvironmentSource()

    return loader.build().loadConfigOrThrow<T>()
}

data class BasicAgentSettings(
    val modelApiKey: String, // option commented out in coral-agent.toml for convenience
    val modelProviderUrl: String, // option commented out in coral-agent.toml for convenience
    val modelId: String, // option commented out in coral-agent.toml for convenience
    val systemPrompt: String,
    val extraSystemPrompt: String,
    val extraInitialUserPrompt: String,
    val followUpUserPrompt: String,
    val maxIterations: Int,
    val coralConnectionUrl: String, // not an option, but comes in through env var anyway
)
use coral_rs::agent::Agent;
use coral_rs::agent_loop::AgentLoop;
use coral_rs::api::generated::types::AgentClaimAmount;
use coral_rs::claim_manager::ClaimManager;
use coral_rs::completion_evaluated_prompt::CompletionEvaluatedPrompt;
use coral_rs::init_tracing;
use coral_rs::mcp_server::McpConnectionBuilder;
use coral_rs::repeating_prompt_stream::repeating_prompt_stream;
use coral_rs::rig::client::{CompletionClient, ProviderClient};
use coral_rs::rig::providers::openrouter;
use reqwest::header::{AUTHORIZATION, HeaderMap, HeaderValue};
use std::time::Duration;

include!(concat!(env!("OUT_DIR"), "/coral_options.rs"));

#[tokio::main]
async fn main() {
    init_tracing().expect("setting default subscriber failed");

    let options = Options::parse().expect("An error occurred parsing the arguments");

    let coral_mcp = McpConnectionBuilder::build_coral_sse()
        .await
        .expect("Failed to connect to the Coral server");

    let devin_mcp = McpConnectionBuilder::builder()
        .build_sse_with_headers(
            "https://mcp.devin.ai/sse",
            HeaderMap::from_iter(
                [(
                    AUTHORIZATION,
                    HeaderValue::from_str(
                        format!(
                            "Bearer {}",
                            std::env::var("DEVIN_API_KEY").expect("DEVIN_API_KEY is required")
                        )
                        .as_str(),
                    )
                    .expect("DEVIN_API_KEY gave invalid header value"),
                )]
                .into_iter(),
            ),
        )
        .await
        .expect("Failed to connect to the Devin MCP server");

    let completion_agent = openrouter::Client::from_env()
        .agent("openai/gpt-5")
        .max_tokens(options.max_tokens as u64)
        .build();

    let mut system_prompt = coral_mcp.prompt_with_resources_str(options.system_prompt);

    if let Some(extra_system_prompt) = options.extra_system_prompt {
        system_prompt = system_prompt.string(extra_system_prompt);
    }

    let claim_manager = ClaimManager::new()
        .mil_input_token_cost(AgentClaimAmount::Usd(1.250))
        .mil_output_token_cost(AgentClaimAmount::Usd(10.000));

    let agent = Agent::new(completion_agent)
        .preamble(system_prompt)
        .claim_manager(claim_manager)
        .mcp_server(coral_mcp.clone())
        .mcp_server(devin_mcp);

    let repeating_user_prompt =
        CompletionEvaluatedPrompt::from_string(options.followup_user_prompt);

    let prompt_stream = repeating_prompt_stream(
        repeating_user_prompt,
        options.iteration_delay.map(Duration::from_secs),
        options.max_iterations as usize,
    );

    AgentLoop::new(agent, prompt_stream)
        .execute()
        .await
        .expect("Agent loop failed");
}

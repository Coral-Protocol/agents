use coral_rs::agent::Agent;
use coral_rs::agent_loop::AgentLoop;
use coral_rs::api::generated::types::AgentClaimAmount;
use coral_rs::claim_manager::ClaimManager;
use coral_rs::completion_evaluated_prompt::CompletionEvaluatedPrompt;
use coral_rs::init_tracing;
use coral_rs::mcp_server::McpConnectionBuilder;
use coral_rs::repeating_prompt_stream::repeating_prompt_stream;
use coral_rs::rig::client::{CompletionClient, ProviderClient};
use coral_rs::rig::providers::openrouter::CLAUDE_3_7_SONNET;
use coral_rs::rig::providers::{anthropic, openai, openrouter};
use std::time::Duration;

include!(concat!(env!("OUT_DIR"), "/coral_options.rs"));

#[tokio::main]
async fn main() {
    init_tracing().expect("setting default subscriber failed");

    let options = Options::parse().expect("An error occurred parsing the arguments");

    let coral_mcp = McpConnectionBuilder::build_coral_streamable_http()
        .await
        .expect("Failed to connect to the Coral server");

    let firecrawl_mcp = McpConnectionBuilder::builder()
        .build_streamable_http(format!(
            "https://mcp.firecrawl.dev/{}/v2/mcp",
            std::env::var("FIRECRAWL_API_KEY").expect("FIRECRAWL_API_KEY is required")
        ))
        .await
        .expect("Failed to connect to the Firecrawl MCP server");

    let completion_agent = anthropic::Client::from_env()
        .agent("claude-sonnet-4-5")
        .max_tokens(options.max_tokens as u64)
        .build();

    let mut system_prompt = CompletionEvaluatedPrompt::from_string(options.system_prompt)
        .all_resources(coral_mcp.clone());

    if let Some(extra_system_prompt) = &options.extra_system_prompt {
        system_prompt = system_prompt.string(extra_system_prompt.to_string())
    }

    let claim_manager = ClaimManager::new()
        .mil_input_token_cost(AgentClaimAmount::Usd(1.250))
        .mil_output_token_cost(AgentClaimAmount::Usd(10.000));

    let agent = Agent::new(completion_agent, system_prompt)
        .claim_manager(claim_manager)
        .mcp_server(coral_mcp.clone())
        .mcp_server(firecrawl_mcp);

    let prompt_stream = repeating_prompt_stream(
        CompletionEvaluatedPrompt::from_string(options.initial_user_prompt),
        CompletionEvaluatedPrompt::from_string(options.followup_user_prompt),
        options.iteration_delay.map(Duration::from_secs),
        options.max_iterations as usize,
    );

    AgentLoop::new(agent, prompt_stream)
        .execute()
        .await
        .expect("Agent loop failed");
}

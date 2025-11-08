#!/usr/bin/env node
/**
 * Skill Auto-Activation Hook for Fakt Compiler Plugin Development
 *
 * This hook analyzes user prompts and suggests relevant skills based on:
 * - Keyword matching (e.g., "Kotlin API", "Metro pattern", "run tests")
 * - Intent pattern matching (regex for user intentions)
 * - Priority levels (critical, high, medium, low)
 *
 * Triggered on: UserPromptSubmit (before Claude sees the prompt)
 * Configuration: .claude/skills/skill-rules.json
 */

import { readFileSync } from 'fs';
import { join } from 'path';

interface HookInput {
    session_id: string;
    transcript_path: string;
    cwd: string;
    permission_mode: string;
    prompt: string;
}

interface PromptTriggers {
    keywords?: string[];
    intentPatterns?: string[];
}

interface SkillRule {
    type: 'guardrail' | 'domain';
    enforcement: 'block' | 'suggest' | 'warn';
    priority: 'critical' | 'high' | 'medium' | 'low';
    description?: string;
    promptTriggers?: PromptTriggers;
}

interface SkillRules {
    version: string;
    description?: string;
    skills: Record<string, SkillRule>;
}

interface MatchedSkill {
    name: string;
    matchType: 'keyword' | 'intent';
    config: SkillRule;
}

async function main() {
    try {
        // Read input from stdin (JSON from Claude Code)
        const input = readFileSync(0, 'utf-8');
        const data: HookInput = JSON.parse(input);
        const prompt = data.prompt.toLowerCase();

        // Load skill rules from project directory
        const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();
        const rulesPath = join(projectDir, '.claude', 'skills', 'skill-rules.json');

        let rules: SkillRules;
        try {
            rules = JSON.parse(readFileSync(rulesPath, 'utf-8'));
        } catch (err) {
            // Silently exit if skill-rules.json doesn't exist (hook is optional)
            process.exit(0);
        }

        const matchedSkills: MatchedSkill[] = [];

        // Check each skill for matches
        for (const [skillName, config] of Object.entries(rules.skills)) {
            const triggers = config.promptTriggers;
            if (!triggers) {
                continue;
            }

            // Keyword matching (case-insensitive substring match)
            if (triggers.keywords) {
                const keywordMatch = triggers.keywords.some(kw =>
                    prompt.includes(kw.toLowerCase())
                );
                if (keywordMatch) {
                    matchedSkills.push({ name: skillName, matchType: 'keyword', config });
                    continue; // Don't double-match on intent if keyword matched
                }
            }

            // Intent pattern matching (regex for user intentions)
            if (triggers.intentPatterns) {
                const intentMatch = triggers.intentPatterns.some(pattern => {
                    const regex = new RegExp(pattern, 'i');
                    return regex.test(prompt);
                });
                if (intentMatch) {
                    matchedSkills.push({ name: skillName, matchType: 'intent', config });
                }
            }
        }

        // Generate output if matches found
        if (matchedSkills.length > 0) {
            let output = '\n';
            output += '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n';
            output += '🎯 FAKT SKILL ACTIVATION CHECK\n';
            output += '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n';

            // Group by priority for better organization
            const critical = matchedSkills.filter(s => s.config.priority === 'critical');
            const high = matchedSkills.filter(s => s.config.priority === 'high');
            const medium = matchedSkills.filter(s => s.config.priority === 'medium');
            const low = matchedSkills.filter(s => s.config.priority === 'low');

            if (critical.length > 0) {
                output += '🚨 CRITICAL SKILLS (HIGHLY RECOMMENDED):\n';
                critical.forEach(s => {
                    output += `  → ${s.name}`;
                    if (s.config.description) {
                        output += ` - ${s.config.description}`;
                    }
                    output += '\n';
                });
                output += '\n';
            }

            if (high.length > 0) {
                output += '📚 RECOMMENDED SKILLS:\n';
                high.forEach(s => {
                    output += `  → ${s.name}`;
                    if (s.config.description) {
                        output += ` - ${s.config.description}`;
                    }
                    output += '\n';
                });
                output += '\n';
            }

            if (medium.length > 0) {
                output += '💡 SUGGESTED SKILLS:\n';
                medium.forEach(s => {
                    output += `  → ${s.name}`;
                    if (s.config.description) {
                        output += ` - ${s.config.description}`;
                    }
                    output += '\n';
                });
                output += '\n';
            }

            if (low.length > 0) {
                output += '📌 OPTIONAL SKILLS:\n';
                low.forEach(s => {
                    output += `  → ${s.name}`;
                    if (s.config.description) {
                        output += ` - ${s.config.description}`;
                    }
                    output += '\n';
                });
                output += '\n';
            }

            output += '💡 TIP: Use the Skill tool to load these skills before proceeding\n';
            output += '   Example: Use Skill tool with skill name (e.g., "kotlin-api-consultant")\n';
            output += '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n';

            // Output to stdout (will be injected into Claude's context)
            console.log(output);
        }

        process.exit(0);
    } catch (err) {
        // Log error but don't block execution
        console.error('❌ Error in skill-activation-prompt hook:', err);
        process.exit(1);
    }
}

main().catch(err => {
    console.error('❌ Uncaught error in skill-activation-prompt hook:', err);
    process.exit(1);
});

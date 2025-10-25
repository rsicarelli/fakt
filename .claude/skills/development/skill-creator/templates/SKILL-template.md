---
name: {skill-name}
description: {trigger-rich-description - max 1024 chars, include "use when" clause, third person, specific keywords}
allowed-tools: [{minimal-set-of-tools}]
---

# {Skill Title}

{One-line mission statement explaining core value proposition}

## Core Mission

{2-3 sentences providing context:
- What problem does this Skill solve?
- Who is it for?
- What makes it valuable?}

## Instructions

### 1. {First Major Step Title}

**{What this step accomplishes}:**

{Detailed substeps:
- Use bullet points or numbered lists
- Be explicit about what to do
- Handle edge cases
- Provide examples}

**{Validation or checks for this step}:**
- [ ] {Check 1}
- [ ] {Check 2}

**{If step fails}:**
```
{Error message format}
{Suggested action}
```

### 2. {Second Major Step Title}

**{Context for this step}:**

{Detailed instructions:
- What to extract/analyze/execute
- Where to find information
- How to handle variations}

**{Code/command examples if applicable}:**
```bash
# Example command
{command} {arguments}
```

**{Expected output}:**
```
{What success looks like}
```

### 3. {Third Major Step Title}

{Continue pattern...}

### N. {Final Step - Output Results}

**{How to format output}:**

```
{Template for structured output}

Status: {status}
Results: {results}
Next steps: {suggestions}
```

## Supporting Files

{List all resources/ files and explain when they're loaded}

Progressive disclosure for detailed documentation:

- **`resources/{file-1}.md`** - {What it contains} (loaded on-demand when {condition})
- **`resources/{file-2}.md`** - {What it contains} (loaded on-demand when {condition})
- **`scripts/{script-1}.sh`** - {What it does}

## Related Skills

{Skills this can compose with:}

This Skill composes with:
- **`{skill-1}`** - {How they work together}
- **`{skill-2}`** - {How they work together}

This Skill enables:
- {Downstream capability 1}
- {Downstream capability 2}

## Best Practices

1. **{Practice 1}** - {Why it matters}
2. **{Practice 2}** - {Why it matters}
3. **{Practice 3}** - {Why it matters}
4. **{Practice 4}** - {Why it matters}
5. **{Practice 5}** - {Why it matters}

## Common Patterns

### Pattern 1: {Pattern Name}
```
{When to use this pattern}
{How to apply it}
{Expected outcome}
```

### Pattern 2: {Pattern Name}
```
{When to use this pattern}
{How to apply it}
{Expected outcome}
```

## Error Handling

**Common errors and solutions:**

### Error 1: {Error Description}
**Symptom:**
```
{Error message or behavior}
```

**Cause:**
{What causes this}

**Solution:**
{How to fix it}

### Error 2: {Error Description}
{Continue pattern...}

## Known Limitations

{Current constraints:}

- ⚠️ {Limitation 1} - {Why it exists, when it will be addressed}
- ⚠️ {Limitation 2} - {Workaround if available}

{Phase-specific limitations if applicable:}
- 🚧 Phase 1: {What's working}
- 🎯 Phase 2: {What's planned}

## Performance Considerations

{If applicable:}

- **Activation latency**: {Expected time}
- **Resource usage**: {What resources this consumes}
- **Scalability**: {How it performs at scale}

## References

{Internal documentation:}
- `.claude/docs/{relevant-doc}.md` - {What it covers}

{External resources if applicable:}
- {External reference 1}
- {External reference 2}

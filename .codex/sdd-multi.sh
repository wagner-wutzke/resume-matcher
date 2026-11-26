#!/bin/bash

# Resolve paths from this script so commands work from any current directory.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
AGENTS_FILE="$SCRIPT_DIR/AGENTS.md"
BASE_SPECS_DIR="$SCRIPT_DIR/specs"

# Helper for Cross-Platform Clipboard Copying
function copy_to_clipboard() {
    if command -v pbcopy &> /dev/null; then
        echo "$1" | pbcopy
        echo "📋 Context successfully copied to macOS clipboard!"
    elif command -v xclip &> /dev/null; then
        echo "$1" | xclip -selection clipboard
        echo "📋 Context successfully copied to Linux clipboard!"
    else
        echo "⚠️  Clipboard utility (pbcopy/xclip) not found. Printing context directly to terminal:"
        echo -e "\n$1\n"
    fi
}

function show_help() {
    echo "======================================================="
    echo "       SDD Multi-Feature Workflow Automator           "
    echo "======================================================="
    echo "Usage: ./.codex/sdd-multi.sh [command] [arguments]"
    echo ""
    echo "Commands:"
    echo "  init <feature-name>           Creates a new spec sub-directory with standard templates"
    echo "  list                          Lists all existing feature spec sub-directories"
    echo "  bundle <feature> <phase>      Bundles .codex/AGENTS.md + the feature's specific phase file"
    echo ""
    echo "Phases (for bundle command):"
    echo "  1 (Requirements) | 2 (Design) | 3 (Tasks) | 4 (Prompt playbook)"
    echo ""
    echo "Examples:"
    echo "  ./.codex/sdd-multi.sh init authentication"
    echo "  ./.codex/sdd-multi.sh bundle authentication 3"
    exit 1
}

# --- COMMAND: INIT ---
if [ "$1" == "init" ] && [ -n "$2" ]; then
    FEATURE_NAME=$2
    TARGET_DIR="$BASE_SPECS_DIR/$FEATURE_NAME"

    echo "📁 Creating spec suite for feature: [$FEATURE_NAME]..."
    mkdir -p "$TARGET_DIR"

    # Initialize placeholders or pull from existing base templates if you have them
    touch "$TARGET_DIR/01-requirements.md"
    touch "$TARGET_DIR/02-design.md"
    touch "$TARGET_DIR/03-tasks.md"

    echo "✅ Successfully initialized '$TARGET_DIR/' with standard SDD file templates."
    exit 0

# --- COMMAND: LIST ---
elif [ "$1" == "list" ]; then
    echo "🔍 Scanning existing feature specifications..."
    if [ ! -d "$BASE_SPECS_DIR" ] || [ -z "$(ls -A "$BASE_SPECS_DIR")" ]; then
        echo "   No feature directories found under '$BASE_SPECS_DIR/'."
        exit 0
    fi

    for dir in "$BASE_SPECS_DIR"/*/; do
        if [ -d "$dir" ]; then
            echo "   • $(basename "$dir")"
        fi
    done
    exit 0

# --- COMMAND: BUNDLE ---
elif [ "$1" == "bundle" ] && [ -n "$2" ] && [ -n "$3" ]; then
    FEATURE_NAME=$2
    PHASE_NUM=$3
    TARGET_DIR="$BASE_SPECS_DIR/$FEATURE_NAME"

    # 1. Validate Feature Directory
    if [ ! -d "$TARGET_DIR" ]; then
        echo "❌ Error: Feature directory '$TARGET_DIR' does not exist."
        echo "💡 Run './.codex/sdd-multi.sh list' to see available features."
        exit 1
    fi

    # 2. Resolve target file based on chosen phase
    case $PHASE_NUM in
        1) SPEC_FILE="$TARGET_DIR/01-requirements.md" ;;
        2) SPEC_FILE="$TARGET_DIR/02-design.md" ;;
        3) SPEC_FILE="$TARGET_DIR/03-tasks.md" ;;
        4) SPEC_FILE="$SCRIPT_DIR/prompts.md" ;;
        *)
           echo "❌ Error: Invalid phase option '$PHASE_NUM'."
           echo "💡 Choose: 1 (Requirements), 2 (Design), 3 (Tasks), or 4 (Prompts)."
           exit 1
           ;;
    esac

    # 3. Validate file existence
    if [ ! -f "$AGENTS_FILE" ]; then
        echo "❌ Error: Global configuration '$AGENTS_FILE' is missing."
        exit 1
    fi

    if [ ! -f "$SPEC_FILE" ]; then
        echo "❌ Error: Target specification file '$(basename "$SPEC_FILE")' missing from '$TARGET_DIR'."
        exit 1
    fi

    # 4. Generate the bundle context string
    echo "📦 Packaging Core Agents Protocol + [$FEATURE_NAME] phase context..."
    BUNDLE_CONTENT=$(cat << EOF
=========================================
SYSTEM RULE PROTOCOLS & CORE AGENT ROLES
=========================================
$(cat "$AGENTS_FILE")

=========================================
ACTIVE SPECIFICATION FILE: $SPEC_FILE
=========================================
$(cat "$SPEC_FILE")

👉 Instruction: Acknowledge the context above. Read the parameters carefully and stand by for execution instructions.
EOF
)

    copy_to_clipboard "$BUNDLE_CONTENT"
    exit 0

else
    show_help
fi

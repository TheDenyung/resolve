# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-03-01

### Added
- ScreenParser test suite (228 LOC, 23 tests)
- ElementIndex test suite (236 LOC, 18 tests)
- StateAnalyzer test suite (303 LOC, 26 tests)
- CompleteActivity Robolectric test suite (298 LOC, 18 tests)
- OnboardingActivity Robolectric test suite (136 LOC, 8 tests)
- OAuthReturnActivity Robolectric test suite (56 LOC, 2 tests)
- SettingsActivity Robolectric test suite (206 LOC, 13 tests)
- SupportAccessibilityService companion test suite (32 LOC, 2 tests)
- CompanionAgentService intent test suite (117 LOC, 5 tests)

### Changed
- Split AccessibilityEngine.kt (1,428 LOC) into 6 focused classes:
  - ScreenParser.kt (310 LOC) -- LLM presentation and formatting
  - ElementIndex.kt (77 LOC) -- element indexing, filtering, deduplication
  - StateAnalyzer.kt (130 LOC) -- pattern detection, fingerprinting, diffing
  - ScreenModels.kt (88 LOC) -- ScreenState, UIElement, ClickableElement data classes
  - GestureExecutor.kt (100 LOC) -- tap, swipe, global actions
  - ActionExecutor.kt (232 LOC) -- composite actions, screen stabilization
- AccessibilityEngine.kt reduced from 1,428 to 589 LOC (facade/coordinator)
- Test heap size increased from 2048m to 4096m to prevent OOM on full suite
- Total test suite: 25 files, 8,176 lines of test code, 648 tests

## [1.1.0] - 2026-03-01

### Added
- AccessibilityEngine test suite with Robolectric (727 LOC)
- PhaseDetector comprehensive test coverage (809 LOC)
- PromptBuilder behavioral tests (433 LOC)
- ConversationManager test suite (425 LOC)
- AgentConfig test coverage (302 LOC)
- Robolectric 4.14.1 dependency for Android resource testing

### Changed
- JaCoCo LINE coverage threshold: 0.20 → 0.35
- JaCoCo BRANCH coverage threshold: 0.15 → 0.25
- Enabled `isIncludeAndroidResources` for Robolectric support
- Total test suite: 16 files, 6,564 lines of test code

## [1.0.0] - 2026-02-28

### Added
- Accessibility agent service with screen content analysis
- ChatGPT OAuth integration for AI-powered assistance
- Floating overlay UI with configurable chat interface
- Order-page detection and guided checkout flow
- Onboarding wizard with permission setup
- Settings screen with authentication management
- 187 unit tests with JaCoCo coverage reporting
- GitHub Actions CI workflow

### Fixed
- Agent restart reliability improvements
- Settings authentication flow redesign
- UX friction points in onboarding

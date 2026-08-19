# Guess Market — Exercise 1 (Console Application)

Java-based software development course, Summer 2026.
A prediction-market system for binary events priced with **LMSR**
(Logarithmic Market Scoring Rule), driven by a console menu.

> The official submission document — submitter details, the full list of
> assumptions and the design decisions required by the assignment — is
> `readme.docx` inside the submission zip handed in through the course system.

## Running

Requires **Java 25** on the `PATH`.

```bat
build.bat      :: compiles both modules into dist\ (needs JAVA_HOME set to a JDK 25)
package.bat    :: builds readme.docx and the submission zip
dist\run.bat   :: starts the application
```

Sample XML files to load are in `test-files\` — both valid ones and deliberately
invalid ones (duplicate event id, commission out of range, wrong extension).

## Menu commands

| # | Command |
|---|---|
| 1 | Load system details file (XML) |
| 2 | Show events |
| 3 | Show event trade status |
| 4 | Buy shares in an event |
| 5 | Close (resolve) an event |
| 6 | Exit |
| 7 | Save system state to file *(bonus)* |
| 8 | Load system state from file *(bonus)* |

Commands 7 and 8 implement the assignment's bonus: the complete system state,
including all trade history, is serialized to an external file and can be
restored later — as opposed to a regular XML load.

## Structure

The system is split into two modules, as required. The UI knows the engine only
through the `Engine` interface and receives immutable DTOs; core objects are
never exposed outside the engine.

```
engine/src/engine/
  api/        Engine (interface) + EngineImpl
  core/       Event (LMSR math), Market, EventOption, Trade, CommissionType
  dto/        Immutable transfer objects returned to the UI
  exception/  Unchecked exceptions with detailed messages
  jaxb/       XML schema mapping
ui/src/ui/    Main, ConsoleUI — the only place that reads input and prints output
docs/         Working notes: spec summary, LMSR formulas, schema, design decisions
```

## LMSR

Option price and cost function, where `q` is the number of shares bought so far
and `b` is the event's liquidity parameter:

```
p_yes = e^(q_yes/b) / ( e^(q_yes/b) + e^(q_no/b) )
C(q_yes, q_no) = b * ln( e^(q_yes/b) + e^(q_no/b) )
```

The cost of a purchase is the difference in `C` before and after it. Verified
against the worked example in the assignment: with `b=100`, buying 100 shares
costs 62.01 and moves the price to 0.73.

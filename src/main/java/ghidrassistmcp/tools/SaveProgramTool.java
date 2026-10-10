package ghidrassistmcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ghidra.app.services.ProgramManager;
import ghidra.framework.model.DomainFile;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Program;
import ghidra.util.Msg;
import ghidra.util.Swing;
import ghidra.util.task.TaskMonitor;
import ghidrassistmcp.GhidrAssistMCPBackend;
import ghidrassistmcp.GhidrAssistMCPManager;
import ghidrassistmcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * eval_python mutations only ever touch the in-memory program; the active
 * transaction it runs inside blocks File -> Save from within the same call,
 * so changes were previously stranded until a human saved from the GUI.
 * This tool runs outside any eval transaction and can commit them to disk.
 */
public class SaveProgramTool implements McpTool {

    @Override
    public String getName() {
        return "save_program";
    }

    @Override
    public String getDescription() {
        return "Save an open program's pending changes to the Ghidra project. If name is omitted, " +
            "saves the current program. Use this after eval_python or other write tools have modified " +
            "a program, since eval_python cannot save from inside its own transaction.";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    public McpSchema.JsonSchema getInputSchema() {
        return new McpSchema.JsonSchema("object",
            Map.of("name", Map.of(
                "type", "string",
                "description", "Open program name or project path to save. If omitted, saves the current program."
            )),
            List.of(), null, null, null);
    }

    @Override
    public McpSchema.CallToolResult execute(Map<String, Object> arguments, Program currentProgram) {
        return execute(arguments, currentProgram, null);
    }

    @Override
    public McpSchema.CallToolResult execute(Map<String, Object> arguments, Program currentProgram,
                                            GhidrAssistMCPBackend backend) {
        GhidrAssistMCPManager manager = GhidrAssistMCPManager.getInstance();
        PluginTool pluginTool = manager.getActiveTool();
        if (pluginTool == null) {
            return textResult("No active Ghidra tool/window available.", true);
        }

        ProgramManager programManager = pluginTool.getService(ProgramManager.class);
        if (programManager == null) {
            return textResult("ProgramManager service not available. Is CodeBrowser open?", true);
        }

        String name = stringArg(arguments.get("name"), null);
        Program target = name == null ? programManager.getCurrentProgram() :
            resolveOpenProgram(programManager.getAllOpenPrograms(), name);
        if (target == null) {
            if (name == null) {
                return textResult("No current program is open.", true);
            }
            return textResult("Open program not found: " + name, true);
        }

        String label = describeProgram(target);
        if (!target.isChanged()) {
            return textResult("No unsaved changes: " + label, false);
        }

        return save(target, label, () -> Swing.runNow(() -> pluginTool.prepareToSave(target)));
    }

    /** Saves on the calling thread and reports the finished outcome. */
    static McpSchema.CallToolResult save(Program target, String label, Runnable prepareToSave) {
        Object consumer = new Object();
        if (!target.addConsumer(consumer)) {
            return textResult("Program was closed before it could be saved: " + label, true);
        }
        try {
            prepareToSave.run();
            target.getDomainFile().save(TaskMonitor.DUMMY);
        } catch (Exception e) {
            Msg.error(SaveProgramTool.class, "Failed to save program: " + label, e);
            return textResult("Failed to save " + label + ": " +
                e.getClass().getSimpleName() + ": " + e.getMessage(), true);
        } finally {
            target.release(consumer);
        }

        if (target.isChanged()) {
            return textResult("Save did not complete: " + label + " still reports unsaved changes.", true);
        }
        return textResult("Saved: " + label, false);
    }

    private Program resolveOpenProgram(Program[] openPrograms, String name) {
        List<Program> exactMatches = new ArrayList<>();
        List<Program> caseInsensitiveMatches = new ArrayList<>();
        List<Program> partialMatches = new ArrayList<>();
        String lowerName = name.toLowerCase();

        for (Program program : openPrograms) {
            String programName = program.getName();
            String path = projectPath(program);
            if (programName.equals(name) || name.equals(path)) {
                exactMatches.add(program);
            }
            else if (programName.equalsIgnoreCase(name) || path.equalsIgnoreCase(name)) {
                caseInsensitiveMatches.add(program);
            }
            else if (programName.toLowerCase().contains(lowerName) ||
                     path.toLowerCase().contains(lowerName)) {
                partialMatches.add(program);
            }
        }

        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }
        if (caseInsensitiveMatches.size() == 1) {
            return caseInsensitiveMatches.get(0);
        }
        if (partialMatches.size() == 1) {
            return partialMatches.get(0);
        }
        return null;
    }

    private String describeProgram(Program program) {
        String path = projectPath(program);
        if (!path.isBlank()) {
            return program.getName() + " (" + path + ")";
        }
        return program.getName();
    }

    private String projectPath(Program program) {
        DomainFile domainFile = program.getDomainFile();
        return domainFile != null ? domainFile.getPathname() : "";
    }

    private String stringArg(Object value, String defaultValue) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return defaultValue;
    }

    private static McpSchema.CallToolResult textResult(String message, boolean error) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(message)
            .isError(error)
            .build();
    }
}

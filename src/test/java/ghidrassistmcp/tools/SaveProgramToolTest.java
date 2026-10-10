package ghidrassistmcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ghidra.framework.model.DomainFile;
import ghidra.program.model.listing.Program;
import io.modelcontextprotocol.spec.McpSchema;

class SaveProgramToolTest {
    @Test void reportsSavedOnlyAfterTheSaveFinishesOnTheCallingThread() {
        FakeProgram program = new FakeProgram();
        var result = SaveProgramTool.save(program.program, "app", () -> program.events.add("prepare"));
        assertNotEquals(Boolean.TRUE, result.isError());
        assertEquals("Saved: app", text(result));
        assertEquals(List.of("prepare", "save"), program.events);
        assertSame(Thread.currentThread(), program.saveThread);
        assertFalse(program.changed);
        assertEquals(0, program.consumers);
    }

    @Test void saveFailureIsAnErrorAndReleasesTheProgram() {
        FakeProgram program = new FakeProgram();
        program.failure = new IOException("Unable to lock due to active transaction");
        var result = SaveProgramTool.save(program.program, "app", () -> {});
        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(text(result).contains("active transaction"));
        assertEquals(0, program.consumers);
    }

    @Test void changesRemainingAfterSaveAreAnError() {
        FakeProgram program = new FakeProgram();
        program.stillChanged = true;
        var result = SaveProgramTool.save(program.program, "app", () -> {});
        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(text(result).startsWith("Save did not complete"));
    }

    @Test void closedProgramIsNotSaved() {
        FakeProgram program = new FakeProgram();
        program.closed = true;
        var result = SaveProgramTool.save(program.program, "app", () -> fail("prepared a closed program"));
        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(program.events.isEmpty());
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    private static final class FakeProgram {
        final List<String> events = new ArrayList<>();
        boolean changed = true, stillChanged, closed;
        int consumers;
        IOException failure;
        Thread saveThread;
        final DomainFile file = proxy(DomainFile.class, name -> {
            if (!name.equals("save")) throw new UnsupportedOperationException(name);
            assertEquals(1, consumers, "save must retain the program");
            saveThread = Thread.currentThread();
            if (failure != null) throw failure;
            events.add("save");
            changed = stillChanged;
            return null;
        });
        final Program program = proxy(Program.class, name -> switch (name) {
            case "addConsumer" -> { if (closed) yield false; consumers++; yield true; }
            case "release" -> { consumers--; yield null; }
            case "getDomainFile" -> file;
            case "isChanged" -> changed;
            default -> throw new UnsupportedOperationException(name);
        });
    }

    @FunctionalInterface private interface Handler { Object handle(String method) throws Exception; }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
            (object, method, args) -> handler.handle(method.getName()));
    }
}

package io.github.wysohn.triggerreactor.bukkit.manager.trigger.share.api.placeholder;

import io.github.wysohn.triggerreactor.core.main.TriggerReactorCore;
import io.github.wysohn.triggerreactor.core.manager.GlobalVariableManager;
import io.github.wysohn.triggerreactor.core.script.interpreter.TemporaryGlobalVariableKey;
import org.bukkit.entity.Player;

import java.util.Map;

public class VariablePlaceholder implements IVariablePlaceholder {
    private final TriggerReactorCore plugin;

    public VariablePlaceholder(TriggerReactorCore plugin) {
        this.plugin = plugin;
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player     A Player who performed the task which contains interaction on
     *                   PlaceholderAPI.
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier == null || identifier.length() == 0 || identifier.equals("?"))
            return "";

        if (identifier.toLowerCase().equals("version")) {
            return plugin.getVersion();
        }

        Map<Object, Object> adapter = plugin.getVariableManager().getGlobalVariableAdapter();

        Object value = null;

        if (identifier.startsWith("?")) {
            //%tr_?<variable name>% - temporary global variable
            String variableName = identifier.substring(1).replace('_', '.');
            TemporaryGlobalVariableKey tempKey = new TemporaryGlobalVariableKey(variableName);

            value = adapter.get(tempKey);
        } else {
            // %tr_<variable name>%
            //if(identifier.contains("")){return "";}
            String variableName = identifier.replace('_', '.');
            GlobalVariableManager vm = plugin.getVariableManager();
            value = vm.get(variableName);
        }

//        if (value == null) {
//            return "";
//        }
//        if (value instanceof Number) {
//            value = String.valueOf(value);
//        }
//
//        if (!(value instanceof String)) {
//            return "";
//        } else {
//            String output = (String) value;
//            return output;
//        }

        return String.valueOf(value == null ? "" : value);
    }
}

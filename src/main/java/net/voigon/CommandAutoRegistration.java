/*
MIT License

Copyright (c) 2022 CodeBase Java Academy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package net.voigon;

import com.google.common.reflect.ClassPath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;

/**
 * Allows command auto registration based on package name.
 * To use, instantiate the class and invoke one of the register method.
 * @author Voigon
 */
@RequiredArgsConstructor
public class CommandAutoRegistration {

    /**
     * Your plugin instance
     */
    @NonNull
    final JavaPlugin
            plugin;

    /**
     * Should commands marked with devServer=true be loaded
     */
    final boolean
            loadDevCommands;

    /**
     * Register all commands in given package, including its sub packages
     * @param packageName given package name
     */
    public void register(String packageName) {
        register(packageName, true);

    }

    /**
     * Register all commands in given package
     * @param packageName given package name
     * @param deep set to true to include sub packages, otherwise will include only classes from given package
     */
    public void register(String packageName, boolean deep) {
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        ClassPath classPath;
        try {
            classPath = ClassPath.from(classLoader);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (ClassPath.ClassInfo classInfo : deep ? classPath.getTopLevelClassesRecursive(packageName) : classPath.getTopLevelClasses(packageName)) {
            Class<?> clazz;
            try {
                clazz = classLoader.loadClass(classInfo.getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            if (!CommandExecutor.class.isAssignableFrom(clazz))
                continue;

            Command annotation = clazz.getAnnotation(Command.class);
            if (annotation == null)
                continue;

            boolean devCommand = annotation.devServer();
            if (devCommand && !loadDevCommands)
                continue;

            CommandExecutor instance = createInstance((Class<? extends CommandExecutor>) clazz);

            PluginCommand pluginCommand = plugin.getCommand(annotation.value());
            if (pluginCommand == null)
                plugin.getLogger().warning("Command /" + annotation.value() + " is not registered to this plugin!");
            else {
                pluginCommand.setExecutor(instance);
                plugin.getLogger().info("Loaded " + (devCommand ? "dev" : "") + " command /" + annotation.value() + "!");
            }

        }

    }

    @SneakyThrows
    private CommandExecutor createInstance(Class<? extends CommandExecutor> clazz) {
        Constructor<?> constructor = clazz.getConstructors()[0];
        return (CommandExecutor) constructor.newInstance(constructor.getParameterCount() == 0 ? new Object[0] : new Object[] {plugin});
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    /*
     * Annotates a type as a command type
     */
    public @interface Command {

        /**
         * Command name, should be lower case per Bukkit's standards
         */
        String value();

        /**
         * True if this listener should only be loaded if loadDevCommands is set to true in CommandAutoRegistration
         */
        boolean devServer() default false;

    }

}
/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui.export;

import javafx.scene.Node;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.mod.ModAdviser;
import org.jackhuang.hmcl.mod.ModpackExportInfo;
import org.jackhuang.hmcl.mod.mcbbs.McbbsModpackExportTask;
import org.jackhuang.hmcl.mod.multimc.MultiMCInstanceConfiguration;
import org.jackhuang.hmcl.mod.multimc.MultiMCModpackExportTask;
import org.jackhuang.hmcl.mod.server.ServerModpackExportTask;
import org.jackhuang.hmcl.mod.modrinth.ModrinthModpackExportTask;
import org.jackhuang.hmcl.setting.Config;
import org.jackhuang.hmcl.setting.FontManager;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.setting.VersionSetting;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.wizard.WizardController;
import org.jackhuang.hmcl.ui.wizard.WizardProvider;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.io.JarUtils;
import org.jackhuang.hmcl.util.io.Zipper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.jackhuang.hmcl.setting.ConfigHolder.config;

public final class ExportWizardProvider implements WizardProvider {
    private final Profile profile;
    private final String version;

    public ExportWizardProvider(Profile profile, String version) {
        this.profile = profile;
        this.version = version;
    }

    @Override
    public void start(Map<String, Object> settings) {
    }

    @Override
    public Object finish(Map<String, Object> settings) {
        @SuppressWarnings("unchecked")
        List<String> whitelist = (List<String>) settings.get(ModpackFileSelectionPage.MODPACK_FILE_SELECTION);
        File modpackFile = (File) settings.get(ModpackInfoPage.MODPACK_FILE);
        ModpackExportInfo exportInfo = (ModpackExportInfo) settings.get(ModpackInfoPage.MODPACK_INFO);
        exportInfo.setWhitelist(whitelist);
        String modpackType = (String) settings.get(ModpackTypeSelectionPage.MODPACK_TYPE);

        return exportWithLauncher(modpackType, exportInfo, modpackFile);
    }

    private Task<?> exportWithLauncher(String modpackType, ModpackExportInfo exportInfo, File modpackFile) {
        Path launcherJar = JarUtils.thisJarPath();
        boolean packWithLauncher = exportInfo.isPackWithLauncher() && launcherJar != null;
        return new Task<Object>() {
            File tempModpack;
            Task<?> exportTask;

            @Override
            public boolean doPreExecute() {
                return true;
            }

            @Override
            public void preExecute() throws Exception {
                File dest;
                if (packWithLauncher) {
                    dest = tempModpack = Files.createTempFile("hmcl", ".zip").toFile();
                } else {
                    dest = modpackFile;
                }

                switch (modpackType) {
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MCBBS:
                        exportTask = exportAsMcbbs(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MULTIMC:
                        exportTask = exportAsMultiMC(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_SERVER:
                        exportTask = exportAsServer(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MODRINTH:
                        exportTask = exportAsModrinth(exportInfo, dest);
                        break;
                    default:
                        throw new IllegalStateException("Unrecognized modpack type " + modpackType);
                }

            }

            @Override
            public Collection<Task<?>> getDependents() {
                return Collections.singleton(exportTask);
            }

            @Override
            public void execute() throws Exception {
                if (!packWithLauncher) return;
                try (Zipper zip = new Zipper(modpackFile.toPath())) {
                    Config exported = new Config();

                    exported.setBackgroundImageType(config().getBackgroundImageType());
                    exported.setBackgroundImage(config().getBackgroundImage());
                    exported.setTheme(config().getTheme());
                    exported.setDownloadType(config().getDownloadType());
                    exported.setPreferredLoginType(config().getPreferredLoginType());
                    exported.getAuthlibInjectorServers().setAll(config().getAuthlibInjectorServers());

                    zip.putTextFile(exported.toJson(), ".hmcl/hmcl.json");
                    zip.putFile(tempModpack, "modpack.zip");

                    Path bg = Metadata.HMCL_CURRENT_DIRECTORY.resolve("background");
                    if (!Files.isDirectory(bg))
                        bg = Metadata.CURRENT_DIRECTORY.resolve("bg");
                    if (Files.isDirectory(bg))
                        zip.putDirectory(bg, ".hmcl/bg");

                    for (String extension : FXUtils.IMAGE_EXTENSIONS) {
                        String fileName = "background." + extension;
                        Path background = Metadata.HMCL_CURRENT_DIRECTORY.resolve(fileName);
                        if (!Files.isRegularFile(background))
                            background = Metadata.CURRENT_DIRECTORY.resolve(fileName);
                        if (Files.isRegularFile(background))
                            zip.putFile(background, ".hmcl/" + fileName);
                    }

                    for (String extension : FontManager.FONT_EXTENSIONS) {
                        String fileName = "font." + extension;
                        Path font = Metadata.HMCL_CURRENT_DIRECTORY.resolve(fileName);
                        if (!Files.isRegularFile(font))
                            font = Metadata.CURRENT_DIRECTORY.resolve(fileName);
                        if (Files.isRegularFile(font))
                            zip.putFile(font, ".hmcl/" + fileName);
                    }

                    zip.putFile(launcherJar, launcherJar.getFileName().toString());
                }
            }
        };
    }

    private Task<?> exportAsMcbbs(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency = null;

            @Override
            public void execute() {
                dependency = new McbbsModpackExportTask(profile.getRepository(), version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsMultiMC(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            @Override
            public void execute() {
                VersionSetting vs = profile.getVersionSetting(version);
                dependency = new MultiMCModpackExportTask(profile.getRepository(), version, exportInfo.getWhitelist(),
                        new MultiMCInstanceConfiguration(
                                "OneSix",
                                exportInfo.getName() + "-" + exportInfo.getVersion(),
                                null,
                                Lang.toIntOrNull(vs.getPermSize()),
                                vs.getWrapper(),
                                vs.getPreLaunchCommand(),
                                null,
                                exportInfo.getDescription(),
                                null,
                                exportInfo.getJavaArguments(),
                                vs.isFullscreen(),
                                vs.getWidth(),
                                vs.getHeight(),
                                vs.getMaxMemory(),
                                exportInfo.getMinMemory(),
                                vs.isShowLogs(),
                                /* showConsoleOnError */ true,
                                /* autoCloseConsole */ false,
                                /* overrideMemory */ true,
                                /* overrideJavaLocation */ false,
                                /* overrideJavaArgs */ true,
                                /* overrideConsole */ true,
                                /* overrideCommands */ true,
                                /* overrideWindow */ true,
                                /* iconKey */ null // TODO
                        ), modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsServer(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            @Override
            public void execute() {
                dependency = new ServerModpackExportTask(profile.getRepository(), version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsModrinth(ModpackExportInfo exportInfo, File modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            @Override
            public void execute() {
                dependency = new ModrinthModpackExportTask(
                    profile.getRepository(),
                    version,
                    exportInfo,
                    modpackFile
                );
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    @Override
    public Node createPage(WizardController controller, int step, Map<String, Object> settings) {
        switch (step) {
            case 0:
                return new ModpackTypeSelectionPage(controller);
            case 1:
                return new ModpackInfoPage(controller, profile.getRepository(), version);
            case 2:
                return new ModpackFileSelectionPage(controller, profile, version, ModAdviser::suggestMod);
            default:
                throw new IllegalArgumentException("step");
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }
}

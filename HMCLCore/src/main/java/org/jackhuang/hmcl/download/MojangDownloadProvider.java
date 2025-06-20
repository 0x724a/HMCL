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
package org.jackhuang.hmcl.download;

import org.jackhuang.hmcl.download.fabric.FabricAPIVersionList;
import org.jackhuang.hmcl.download.fabric.FabricVersionList;
import org.jackhuang.hmcl.download.forge.ForgeVersionList;
import org.jackhuang.hmcl.download.game.GameVersionList;
import org.jackhuang.hmcl.download.liteloader.LiteLoaderVersionList;
import org.jackhuang.hmcl.download.neoforge.NeoForgeOfficialVersionList;
import org.jackhuang.hmcl.download.optifine.OptiFine302VersionList;
import org.jackhuang.hmcl.download.quilt.QuiltAPIVersionList;
import org.jackhuang.hmcl.download.quilt.QuiltVersionList;

/**
 * @author huangyuhui
 * @see <a href="http://wiki.vg">http://wiki.vg</a>
 */
public class MojangDownloadProvider implements DownloadProvider {
    private final GameVersionList game;
    private final FabricVersionList fabric;
    private final FabricAPIVersionList fabricApi;
    private final ForgeVersionList forge;
    private final NeoForgeOfficialVersionList neoforge;
    private final LiteLoaderVersionList liteLoader;
    private final OptiFine302VersionList optifine;
    private final QuiltVersionList quilt;
    private final QuiltAPIVersionList quiltApi;

    public MojangDownloadProvider() {
        // If there is no official download channel available, fallback to BMCLAPI.
        String apiRoot = "https://bmclapi2.bangbang93.com";

        this.game = new GameVersionList(this);
        this.fabric = new FabricVersionList(this);
        this.fabricApi = new FabricAPIVersionList(this);
        this.forge = new ForgeVersionList(this);
        this.neoforge = new NeoForgeOfficialVersionList(this);
        this.liteLoader = new LiteLoaderVersionList(this);
        this.optifine = new OptiFine302VersionList("https://hmcl-dev.github.io/metadata/optifine/");
        this.quilt = new QuiltVersionList(this);
        this.quiltApi = new QuiltAPIVersionList(this);
    }

    @Override
    public String getVersionListURL() {
        return "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    }

    @Override
    public String getAssetBaseURL() {
        return "https://resources.download.minecraft.net/";
    }

    @Override
    public VersionList<?> getVersionListById(String id) {
        switch (id) {
            case "game":
                return game;
            case "fabric":
                return fabric;
            case "fabric-api":
                return fabricApi;
            case "forge":
                return forge;
            case "neoforge":
                return neoforge;
            case "liteloader":
                return liteLoader;
            case "optifine":
                return optifine;
            case "quilt":
                return quilt;
            case "quilt-api":
                return quiltApi;
            default:
                throw new IllegalArgumentException("Unrecognized version list id: " + id);
        }
    }

    @Override
    public String injectURL(String baseURL) {
        return baseURL;
    }

    @Override
    public int getConcurrency() {
        return 6;
    }
}

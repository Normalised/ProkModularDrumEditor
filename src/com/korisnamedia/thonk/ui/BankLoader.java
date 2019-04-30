package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ConfigKeys;
import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.ProkModule;
import com.prokmodular.comms.ParamMessage;
import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetFile;
import com.prokmodular.model.PresetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.data.JSONObject;

import java.io.File;
import java.util.List;

public class BankLoader {
    final Logger logger = LoggerFactory.getLogger(BankLoader.class);

    private final ThonkModularApp app;
    private ProkModule module;
    private PresetManager presetManager;

    public BankLoader(ThonkModularApp appToUse) {
        app = appToUse;
        presetManager = new PresetManager();
    }

    public void load(ProkModule moduleToUse) {
        module = moduleToUse;
        logger.debug("Load bank into module " + module.getConnectionKey());

        File bankFolder = null;

        if(app.getConfig().hasKey(ConfigKeys.BANK_FOLDER)) {
            JSONObject bankFolderConfig = app.getConfig().getJSONObject(ConfigKeys.BANK_FOLDER);
            if(bankFolderConfig.hasKey(module.getConnectionKey())) {
                bankFolder = new File(bankFolderConfig.getString(module.getConnectionKey()));
                if(!bankFolder.exists()) {
                    bankFolder = null;
                } else {
                    logger.debug("Got path from config " + bankFolder.getAbsolutePath());
                }
            }
        } else {
            app.getConfig().setJSONObject(ConfigKeys.BANK_FOLDER, new JSONObject());
        }
        app.selectFolder("Choose Bank Folder", "bankFolderSelected", bankFolder, this);
    }

    public void bankFolderSelected(File selectedFolder) {
        if(selectedFolder == null) return;

        logger.debug("Selected folder " + selectedFolder.getAbsolutePath() + " for " + module.getConnectionKey());

        presetManager.setCurrentModel(module.model);
        List<PresetFile> files = presetManager.listFilesFrom(selectedFolder);
        int numFiles = files.size();
        if(numFiles > 16) numFiles = 16;
        int paramIndex = 0;
        for(int i=0;i<numFiles;i++) {
            try {
                Preset p  = presetManager.readPreset(files.get(i).file);
                paramIndex = 0;
                for(Float f : p.params) {
                    module.setParam(new ParamMessage(paramIndex++, f));
                }
                logger.debug("Saving " + files.get(i).file.getName() + " into " + i);

                module.saveModel(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JSONObject bankFolderConfig = app.getConfig().getJSONObject(ConfigKeys.BANK_FOLDER);
        bankFolderConfig.setString(module.getConnectionKey(), selectedFolder.getAbsolutePath());
    }
}

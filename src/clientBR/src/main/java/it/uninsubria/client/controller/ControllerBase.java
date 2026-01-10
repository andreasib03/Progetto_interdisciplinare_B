package it.uninsubria.client.controller;


import it.uninsubria.client.rmi.ClientServiceManager;
import it.uninsubria.client.utils.classesUI.LanguageManager;


public abstract class ControllerBase implements LanguageManager.LanguageChangeListener {

    protected static ClientServiceManager serviceManager;


    public ControllerBase() {
        // Register this controller as a language change listener
        LanguageManager.addLanguageChangeListener(this);
    }

    public static ClientServiceManager getServiceManager(){
        return serviceManager;
    }

    protected void executeCallback(Runnable callback) {
        if(callback != null) callback.run();
    }

    /**
     * Risolve una chiave di localizzazione. Se inizia con %, usa il bundle delle properties,
     * altrimenti restituisce la stringa così com'è.
     */
    protected static String resolveString(String key) {
        if (key != null && key.startsWith("%")) {
            String actualKey = key.substring(1); // Rimuovi il %
            try {
                return LanguageManager.getBundle().getString(actualKey);
            } catch (Exception e) {
                // Se la chiave non esiste, restituisci un fallback che può essere formattato con MessageFormat
                // Aggiungiamo ": {0}" alla fine per garantire che MessageFormat possa inserire il parametro
                if (!actualKey.contains("{0}")) {
                    return actualKey + ": {0}";
                }
                return actualKey;
            }
        }
        return key;
    }

    /**
     * Called when the application language changes.
     * Subclasses should override this method to refresh their UI texts.
     * @param newLanguage the new language code (e.g., "en", "it")
     */
    @Override
    public void onLanguageChanged(String newLanguage) {
        // Default implementation does nothing
        // Subclasses should override to refresh their specific UI elements
    }

}

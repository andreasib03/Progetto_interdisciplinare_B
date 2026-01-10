package it.uninsubria.client.utils.classesUI;

import java.util.Map;

/**
 * Interface for controllers that can be initialized with parameters.
 */
public interface ParametrizedController {
    /**
     * Initializes the controller with the given parameters.
     *
     * @param params the initialization parameters
     */
    void initData(Map<String, Object> params);
}

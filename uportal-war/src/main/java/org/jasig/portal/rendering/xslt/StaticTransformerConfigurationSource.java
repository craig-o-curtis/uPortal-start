/**
 * Copyright (c) 2000-2010, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */

package org.jasig.portal.rendering.xslt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.portal.spring.spel.IPortalSpELService;
import org.jasig.portal.utils.cache.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class StaticTransformerConfigurationSource implements TransformerConfigurationSource {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private Properties outputProperties;
    private LinkedHashMap<String, Object> parameters;
    private IPortalSpELService portalSpELService;
    private Map<String, Expression> parameterExpressions;
    private Set<String> cacheKeyExcludedParameters = Collections.emptySet();


    @Autowired
    public void setPortalSpELService(IPortalSpELService portalSpELService) {
        this.portalSpELService = portalSpELService;
    }
    
    public void setProperties(Properties transformerOutputProperties) {
        this.outputProperties = transformerOutputProperties;
    }

    public void setParameters(Map<String, Object> transformerParameters) {
        this.parameters = new LinkedHashMap<String, Object>(transformerParameters);
    }

    public void setParameterExpressions(Map<String, String> parameterExpressions) {
        final Map<String, Expression> parameterExpressionsBuilder = new LinkedHashMap<String, Expression>();
        
        for (final Map.Entry<String, String> expressionEntry : parameterExpressions.entrySet()) {
            final String string = expressionEntry.getValue();
            final Expression expression = this.portalSpELService.parseExpression(string);
            parameterExpressionsBuilder.put(expressionEntry.getKey(), expression);
        }
        
        this.parameterExpressions = parameterExpressionsBuilder;
    }
    
    /**
     * Parameter keys to exclude from the cache key.
     */
    public void setCacheKeyExcludedParameters(Set<String> cacheKeyExcludedParameters) {
        this.cacheKeyExcludedParameters = cacheKeyExcludedParameters;
    }

    @Override
    public CacheKey getCacheKey(HttpServletRequest request, HttpServletResponse response) {
        final LinkedHashMap<String, Object> transformerParameters = this.getParameters(request, response);
        transformerParameters.keySet().removeAll(this.cacheKeyExcludedParameters);
        return new CacheKey(this.outputProperties, transformerParameters);
    }

    @Override
    public Properties getOutputProperties(HttpServletRequest request, HttpServletResponse response) {
        this.logger.debug("Returning output properties: {}", this.outputProperties);
        return this.outputProperties;
    }

    @Override
    public LinkedHashMap<String, Object> getParameters(HttpServletRequest request, HttpServletResponse response) {
        final ServletWebRequest webRequest = new ServletWebRequest(request, response);
        
        final LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>(this.parameters);
        
        if (this.parameterExpressions != null) {
            for (final Map.Entry<String, Expression> expressionEntry : this.parameterExpressions.entrySet()) {
                final Expression expression = expressionEntry.getValue();
                final Object value = this.portalSpELService.getValue(expression, webRequest);
                
                parameters.put(expressionEntry.getKey(), value);
            }
        }
        
        this.logger.debug("Returning transformer parameters: {}", this.parameters);
        
        return parameters;
    }
}
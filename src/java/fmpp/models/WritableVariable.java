package fmpp.models;

import freemarker.template.TemplateModel;

/**
 * Marker interface for writable variables. 
 */
abstract class WritableVariable implements Cloneable, TemplateModel {
    public abstract Object clone();
}

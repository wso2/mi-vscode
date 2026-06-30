/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WSO2 LLC - support for WSO2 Micro Integrator Configuration
 */

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorConnection;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorConnectionPool;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorConnectionPoolProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorStatement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorStatementParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorStatementResult;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public abstract class DBMediatorSerializer extends AbstractMediatorSerializer {

    protected OMElement serializeDBMediator(DbMediator dbMediator) {

        OMElement dbMediatorElt = fac.createOMElement("db", synNS);

        serializeAttributes(dbMediator, dbMediatorElt);
        serializeChildren(dbMediator, dbMediatorElt);
        return dbMediatorElt;
    }

    private void serializeAttributes(DbMediator dbMediator, OMElement dbMediatorElt) {

        if (dbMediator.isUseTransaction()) {
            dbMediatorElt.addAttribute("useTransaction", "true", null);
        }
        if (dbMediator.getDescription() != null) {
            dbMediatorElt.addAttribute("description", dbMediator.getDescription(), null);
        }
    }

    private void serializeChildren(DbMediator dbMediator, OMElement dbMediatorElt) {

        if (dbMediator.getConnection() != null) {
            serializeConnection(dbMediator.getConnection(), dbMediatorElt);
        }
        if (dbMediator.getStatement() != null) {
            serializeStatements(dbMediator.getStatement(), dbMediatorElt);
        }
    }

    private void serializeConnection(DbMediatorConnection connection, OMElement dbMediatorElt) {

        OMElement connectionElt = fac.createOMElement("connection", synNS);

        if (connection.getPool() != null) {
            OMElement poolElt = fac.createOMElement("pool", synNS);
            serializeConnectionPool(connection.getPool(), poolElt);
            connectionElt.addChild(poolElt);
        } else {
            handleException("Connection pool is required for DB mediator");
        }

        dbMediatorElt.addChild(connectionElt);

    }

    private void serializeConnectionPool(DbMediatorConnectionPool pool, OMElement connectionElt) {

        if (pool.getDsName() != null) {
            OMElement dsNameElt = fac.createOMElement("dsName", synNS);
            if (pool.getDsName().getValue() != null) {
                dsNameElt.addAttribute("value", pool.getDsName().getValue(), null);
            } else {
                dsNameElt.setText(pool.getDsName().getTextNode());
            }
            connectionElt.addChild(dsNameElt);
        }

        if (pool.getIcClass() != null) {
            OMElement icClassElt = fac.createOMElement("icClass", synNS);
            if (pool.getIcClass().getValue() != null) {
                icClassElt.addAttribute("value", pool.getIcClass().getValue(), null);
            } else {
                icClassElt.setText(pool.getIcClass().getTextNode());
            }
            connectionElt.addChild(icClassElt);
        }

        if (pool.getDriver() != null) {
            OMElement driverElt = fac.createOMElement("driver", synNS);
            if (pool.getDriver().getValue() != null) {
                driverElt.addAttribute("value", pool.getDriver().getValue(), null);
            } else {
                driverElt.setText(pool.getDriver().getTextNode());
            }
            connectionElt.addChild(driverElt);
        }

        if (pool.getUrl() != null) {
            OMElement urlElt = fac.createOMElement("url", synNS);
            if (pool.getUrl().getValue() != null) {
                urlElt.addAttribute("value", pool.getUrl().getValue(), null);
            } else {
                urlElt.setText(pool.getUrl().getTextNode());
            }
            connectionElt.addChild(urlElt);
        }

        if (pool.getUser() != null) {
            OMElement userElt = fac.createOMElement("user", synNS);
            if (pool.getUser().getValue() != null) {
                userElt.addAttribute("value", pool.getUser().getValue(), null);
            } else {
                userElt.setText(pool.getUser().getTextNode());
            }
            connectionElt.addChild(userElt);
        }

        if (pool.getPassword() != null) {
            OMElement passwordElt = fac.createOMElement("password", synNS);
            if (pool.getPassword().getValue() != null) {
                passwordElt.addAttribute("value", pool.getPassword().getValue(), null);
            } else {
                passwordElt.setText(pool.getPassword().getTextNode());
            }
            connectionElt.addChild(passwordElt);
        }

        if (pool.getProperty() != null) {
            serializeProperties(pool.getProperty(), connectionElt);
        }
    }

    private void serializeProperties(DbMediatorConnectionPoolProperty[] properties, OMElement connectionElt) {

        for (DbMediatorConnectionPoolProperty property : properties) {
            OMElement propertyElt = serializeProperty(property);
            connectionElt.addChild(propertyElt);
        }
    }

    private OMElement serializeProperty(DbMediatorConnectionPoolProperty property) {

        OMElement propertyElt = fac.createOMElement("property", synNS);
        if (property.getName() != null) {
            propertyElt.addAttribute("name", property.getName().name(), null);
        }
        if (property.getValue() != null) {
            propertyElt.addAttribute("value", property.getValue(), null);
        }
        return propertyElt;
    }

    private void serializeStatements(DbMediatorStatement[] statement, OMElement dbMediatorElt) {

        for (DbMediatorStatement stmt : statement) {
            OMElement statementElt = serializeStatement(stmt);
            dbMediatorElt.addChild(statementElt);
        }
    }

    private OMElement serializeStatement(DbMediatorStatement stmt) {

        OMElement statementElt = fac.createOMElement("statement", synNS);
        if (stmt.getSql() != null) {
            OMElement sqlElt = fac.createOMElement("sql", synNS);
            String sql = stmt.getSql();
            if (sql.startsWith("<![CDATA[")) {
                OMText cdata = SerializerUtils.stringToCDATA(sql);
                sqlElt.addChild(cdata);
            } else {
                sqlElt.setText(stmt.getSql());
            }
            statementElt.addChild(sqlElt);
        }
        if (stmt.getParameter() != null) {
            serializeParameters(stmt.getParameter(), statementElt);
        }
        if (stmt.getResult() != null) {
            serializeResults(stmt.getResult(), statementElt);
        }

        return statementElt;
    }

    private void serializeParameters(DbMediatorStatementParameter[] parameter, OMElement statementElt) {

        for (DbMediatorStatementParameter param : parameter) {
            OMElement paramElt = serializeParameter(param);
            statementElt.addChild(paramElt);
        }
    }

    private OMElement serializeParameter(DbMediatorStatementParameter param) {

        OMElement paramElt = fac.createOMElement("parameter", synNS);
        if (param.getType() != null) {
            paramElt.addAttribute("type", param.getType().name(), null);
        }
        if (param.getValue() != null) {
            paramElt.addAttribute("value", param.getValue(), null);
        } else if (param.getExpression() != null) {
            SerializerUtils.serializeExpression(param.getExpression(), paramElt, "expression", param);
        }
        return paramElt;
    }

    private void serializeResults(DbMediatorStatementResult[] result, OMElement statementElt) {

        for (DbMediatorStatementResult res : result) {
            OMElement resultElt = serializeResult(res);
            statementElt.addChild(resultElt);
        }
    }

    private OMElement serializeResult(DbMediatorStatementResult res) {

        OMElement resultElt = fac.createOMElement("result", synNS);
        if (res.getColumn() != null) {
            resultElt.addAttribute("column", res.getColumn(), null);
        }
        if (res.getName() != null) {
            resultElt.addAttribute("name", res.getName(), null);
        }
        return resultElt;
    }
}

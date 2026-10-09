/**
 * Manual debug script — run with:
 *   node tests/utils/manual.mjs
 *
 * Requires a build first: npm run build
 */
import { getLanguageService } from '../../dist/index.js'

const ls = getLanguageService()

const xml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://ws.apache.org/ns/synapse">
    
    <api name="CustomerAPI" context="/customers" version="2.0">
        <resource methods="GET POST" uri-template="/{id}">
            <inSequence>
            

                <!-- Variable mediator (new in 4.4) -->
                <variable name="customerId" expression="$func:id" type="STRING"/>
                <variable name="startTime" value="current" type="STRING"/>

                <log level="custom">
                    <property name="CustomerID" expression="$ctx:customerId"/>
                </log>

                <!-- payloadFactory -->
                <payloadFactory media-type="json">
                    
                    <format>{"customerId": "$1", "status": "processed", "time": "$2"}</format>
                    <args>
                        <arg evaluator="xml" expression="$ctx:customerId"/>
                        <arg evaluator="xml" expression="$ctx:startTime"/>
                    </args>
                </payloadFactory>

                <!-- Correct ThrowError mediator (4.4+) -->
                <filter source="$ctx:customerId" regex="^$">
                    <then>
                        <throwError type="VALIDATION_ERROR" 
                                    errorMessage="Customer ID is required"/>
                    </then>
                </filter>

                <respond/>
            </inSequence>

            <outSequence>
                <respond/>
            </outSequence>
        </resource>
    </api>

</definitions>`

const doc = ls.parseXMLDocument('file:///test.xml', xml)

console.log('=== printTreeAST ===')
console.log(ls.printTreeAST(doc))

// console.log('\n=== printAST ===')
// console.log(ls.printAST(doc))

// console.log('\n=== printAST (with positions) ===')
// console.log(ls.printAST(doc, { includePositions: true }))

// console.log('\n=== printCST ===')
// console.log(ls.printCST(doc))

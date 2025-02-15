package org.colchain.colchain.sparql.iter;

import org.colchain.index.index.IndexMapping;
import org.colchain.colchain.node.AbstractNode;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIter1;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

import java.util.ArrayList;
import java.util.List;

public class QueryIterColchainBlock extends QueryIter1 {
    private BasicPattern pattern;
    private QueryIterator output;
    private IndexMapping mapping;

    public static QueryIterator create(QueryIterator input, BasicPattern pattern, ExecutionContext execContext) {
        return new QueryIterColchainBlock(input, pattern, execContext);
    }

    private QueryIterColchainBlock(QueryIterator input, BasicPattern pattern, ExecutionContext execContext) {
        super(input, execContext);
        this.pattern = pattern;

        /*ReorderTransformation reorder = ReorderLib.fixed();
        if (pattern.size() >= 2 && !input.isJoinIdentity()) {
            QueryIterPeek peek = QueryIterPeek.create(input, execContext);
            input = peek;
            Binding b = peek.peek();
            BasicPattern bgp2 = Substitute.substitute(pattern, b);
            ReorderProc reorderProc = reorder.reorderIndexes(bgp2);
            pattern = reorderProc.reorder(pattern);
        }*/
        QueryIterator chain = input;

        List<org.colchain.index.util.Triple> triples = new ArrayList<>();
        for (Triple triple : pattern) {
            triples.add(new org.colchain.index.util.Triple(triple.getSubject().toString(), triple.getPredicate().toString(), triple.getObject().toString()));
        }
        mapping = AbstractNode.getState().getIndex().getMapping(triples);

        for (Triple triple : pattern) {
            chain = new QueryIterColchain(chain, triple, execContext,
                    mapping.getMapping(new org.colchain.index.util.Triple(triple.getSubject().toString(),
                            triple.getPredicate().toString(),
                            triple.getObject().toString())));
        }

        this.output = chain;
    }

    protected boolean hasNextBinding() {
        return this.output.hasNext();
    }

    protected Binding moveToNextBinding() {
        return this.output.nextBinding();
    }

    protected void closeSubIterator() {
        if (this.output != null) {
            this.output.close();
        }

        this.output = null;
    }

    protected void requestSubCancel() {
        if (this.output != null) {
            this.output.cancel();
        }

    }

    protected void details(IndentedWriter out, SerializationContext sCxt) {
        out.print(Lib.className(this));
        out.println();
        out.incIndent();
        FmtUtils.formatPattern(out, this.pattern, sCxt);
        out.decIndent();
    }
}

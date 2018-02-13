/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.parser;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;


/**
 *
 * @author Nika
 */
public class ModASTParallelFor extends VoidVisitorAdapter {

    int beginline = 0;
    String lowerbound = "";
    String upperbound = "";
    String max = "";
    String var = "";
    int vartype = 0;
    String value = "";

    public ModASTParallelFor(int beginline, int Type, String LB, String UB, String MAX) {
        this.beginline = beginline;
        lowerbound = LB;
        upperbound = UB;
        vartype = Type;
        max = MAX;
    }

    public void visit(BinaryExpr n, Object arg) {

        if (n.getBegin().get().line == beginline) {
            switch (vartype) {
                case 0:
                    IntegerLiteralExpr init = new IntegerLiteralExpr();
                    init.setValue(upperbound);
                    n.setRight(init);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;
                case 1:
                    IntegerLiteralExpr init1 = new IntegerLiteralExpr();
                    init1.setValue(upperbound);
                    n.setRight(init1);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;
                case 2:
                    IntegerLiteralExpr init2 = new IntegerLiteralExpr();//(IntegerLiteralExpr) n.getRight();
                    init2.setValue(upperbound);
                    n.setRight(init2);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;
                case 3:
                    LongLiteralExpr init3 = new LongLiteralExpr();
                    init3.setValue(upperbound);
                    n.setRight(init3);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;
                case 4:
                    DoubleLiteralExpr init4 = new DoubleLiteralExpr();
                    init4.setValue(upperbound);
                    n.setRight(init4);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;
                case 5:
                    DoubleLiteralExpr init5 = new DoubleLiteralExpr();
                    init5.setValue(upperbound);
                    n.setRight(init5);
                    if (n.getOperator() == BinaryExpr.Operator.LESS) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.LESS_EQUALS);
                        }
                    } else if (n.getOperator() == BinaryExpr.Operator.GREATER) {
                        if (!upperbound.trim().equalsIgnoreCase(max.trim())) {
                            n.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
                        }
                    }
                    break;

            }
        }
        super.visit(n, arg);
    }

    public void visit(VariableDeclarator n, Object arg) {

        if (n.getBegin().get().line == beginline) {
            switch (vartype) {
                case 0:
                    IntegerLiteralExpr init = new IntegerLiteralExpr();
                    init.setValue(lowerbound);
                    n.setInitializer(init);
                    break;
                case 1:
                    IntegerLiteralExpr init1 = new IntegerLiteralExpr();
                    init1.setValue(lowerbound);
                    n.setInitializer(init1);
                    break;
                case 2:
                    IntegerLiteralExpr init2 = new IntegerLiteralExpr();
                    init2.setValue(lowerbound);
                    n.setInitializer(init2);
                    break;
                case 3:
                    LongLiteralExpr init3 = new LongLiteralExpr();
                    init3.setValue(lowerbound);
                    n.setInitializer(init3);
                    break;
                case 4:
                    DoubleLiteralExpr init4 = new DoubleLiteralExpr();
                    init4.setValue(lowerbound);
                    n.setInitializer(init4);
                    break;
                case 5:
                    DoubleLiteralExpr init5 = new DoubleLiteralExpr();
                    init5.setValue(lowerbound);
                    n.setInitializer(init5);
                    break;

            }
        }
        super.visit(n, arg);

    }

}

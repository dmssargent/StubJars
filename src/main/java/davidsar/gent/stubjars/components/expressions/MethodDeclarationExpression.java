package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.Utils;
import davidsar.gent.stubjars.components.JarMethod;
import davidsar.gent.stubjars.components.JarType;
import davidsar.gent.stubjars.components.writer.Constants;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclarationExpression extends Expression implements FormattedExpression {
    private final List<Expression> children;

    public MethodDeclarationExpression(List<Expression> children) {
        this.children = children;
    }

    public static MethodDeclarationExpression from(JarMethod method, boolean isEnumField) {
        // Figure out method signature
        List<Expression> signature = new ArrayList<>();

        if (!method.getParentClazz().isInterface()) {
            signature.add(method.security().expression());
        }

        if (method.isFinal()) {
            signature.add(StringExpression.FINAL);
        }

        if (method.isStatic()) {
            signature.add(StringExpression.STATIC);
        }

        if (method.isAbstract() && !(method.getParentClazz().isInterface() || isEnumField)) {
            signature.add(StringExpression.ABSTRACT);
        }

        // Convert method type parameters
        signature.add(JarType.convertTypeParametersToExpression(method.typeParameters(), method.getParentClazz()));
        // Convert method return type
        signature.add(JarType.toExpression(method.genericReturnType(), method.getParentClazz()));
        // Convert method name
        signature.add(new MethodSignatureExpression(Expressions.fromString(method.name()), method.parameters()).getFormattedString());
        if (method.requiresThrowsSignature()) {
            signature.add(Expressions.of(StringExpression.THROWS, StringExpression.SPACE, Utils.arrayToListExpression(method.throwsTypes(), x -> JarType.toExpression(x, method.getParentClazz()))));
        }

        return new MethodDeclarationExpression(signature);

    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public List<Expression> children() {
        return children;
    }

    @Override
    public Expression getFormattedString() {
        StringBuilder builder = new StringBuilder();
        for (Expression child : children) {
            if (child.equals(StringExpression.EMPTY)) {
                continue;
            }

            builder.append(child.toString());
            builder.append(Constants.SPACE);
        }
        if (builder.substring(builder.length() - 1).equals(Constants.SPACE)) {
            builder.replace(builder.length() - 1, builder.length(), "");
        }
        return Expressions.fromString(builder.toString().trim());
    }
}

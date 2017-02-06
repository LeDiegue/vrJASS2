package ruke.vrj.translator;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * MIT License
 *
 * <p>Copyright (c) 2017 Franco Montenegro
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class LoopStatement implements Expression {

  public final ImmutableList<Expression> statements;

  public LoopStatement(final Collection<Expression> statements) {
    this.statements = ImmutableList.copyOf(statements);
  }

  @Override
  public final String toString() {
    final Collection<String> body = new ArrayList<>(this.statements.size());

    for (final Expression statement : this.statements) {
      body.add(statement.toString());
    }

    return String.format("loop\n" + "%s\n" + "endloop", String.join("\n", body));
  }
}

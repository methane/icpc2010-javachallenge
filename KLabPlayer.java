package jp.ac.nii.icpc2010.players;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.math.BigInteger;

import jp.ac.nii.icpc2010.players.BasePlayer;
import jp.ac.nii.icpc2010.playfield.FieldDirection;
import jp.ac.nii.icpc2010.playfield.IPlayField;

/**
 * Player created by KLab Inc.
 * 
 * @author yukino
 *
 */

public class KLabPlayer extends BasePlayer
{
    static final long MAX_SCORE = 100000000; 
    static final long MIN_SCORE =-100000000;
    public static final class Field
    {
        private byte[] map; // flag is set where can't to move.
        private int width;
        private int height;

        public Field(int w, int h) {
            map = new byte[h*w];
            width = w;
            height = h;
        }
        public Field(Field other) {
            width = other.width;
            height = other.height;
            map = (byte[])other.map.clone();
        }
        public Object clone() {
            return new Field(this);
        }
        public boolean getAt(int x, int y) {
            int pos = y * width + x;
            return map[pos] != 0;
        }
        public void setAt(int x, int y) {
            int pos = y * width + x;
            map[pos] = 1;
        }

        public static Field fromPlayField(IPlayField playField) {
            int width = playField.getWidth();
            int height = playField.getHeight();
            Field f = new Field(width, height);

            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int obj = playField.getObjectAt(x, y);
                    if (obj != IPlayField.FIELD_FREE && obj != IPlayField.FIELD_COIN) {
                        f.setAt(x, y);
                    }
                }
            }
            return f;
        }
        public String toString() {
            String s = new String();
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    if (getAt(x, y)) {
                        s += '@';
                    } else {
                        s += '.';
                    }
                }
                s += "\r\n";
            }
            return s;
        }
    }

    static class State
    {
        public Field field;
        public int[] positions;
        /// null means not searched yet.
        /// empty means dead end.
        public ArrayList<State> children;
        int coin;

        public State(Field field, int[] positions) {
            this.field = field;
            this.positions = positions;
            this.children = null;
            this.coin = 0;
        }

        FieldDirection getMovement(int id, State prev_state) {
            int x2 = positions[id*2];
            int y2 = positions[id*2+1];
            int x = prev_state.positions[id*2];
            int y = prev_state.positions[id*2+1];

            if (x == x2) {
                if ((y + 1) % KLabPlayer.height == y2) {
                    return FieldDirection.Down;
                }
                return FieldDirection.Up;
            }
            else if ((x + 1) % KLabPlayer.width == x2) {
                return FieldDirection.Right;
            }
            return FieldDirection.Left;
        }
    }

    static private int width, height;
    static private int numPlayers;
    private int state_count_limit;

	public KLabPlayer(int id, IPlayField playField) {
		super(id, playField);
        width = playField.getWidth();
        height = playField.getHeight();
        numPlayers = playField.getNumOfPlayers();
        state_count_limit = 10000 / (width * height) + 1;
        System.out.print("ID: ");
        System.out.println(id);
        System.out.println(state_count_limit);
	}
	FieldDirection prev_dir;

    boolean got_coin;
    private Field tryMove(Field field, int x, int y, int id_, int[] pos_) {
        got_coin = false;
        x = (x + width) % width;
        y = (y + height) % height;

        if (field.getAt(x, y)) {
            int i;
            for (i = 0; i < id_; ++i) {
                if (pos_[i*2] == x && pos_[i*2+1] == y) {
                    pos_[i*2] = pos_[i*2+1] = -1;
                    break;
                }
            }
            if (i == id_)
                return null;
            x = y = -1;
        }
        Field newfield = (Field)field.clone();
        if (x >= 0) {
            got_coin = isCoin(x, y);
            newfield.setAt(x, y);
        }
        pos_[id_*2] = x;
        pos_[id_*2+1] = y;
        return newfield;
    }

    long state_count;
    private void checkTroyMove(State parent, Field field, int[] positions,
                               ArrayDeque<State> queue, int next_id, int coin)
    {
        // todo: collision detect.
        if (next_id >= numPlayers) {
            state_count++;
            State s = new State(field, (int[])positions.clone());
            queue.addLast(s);
            s.coin = parent.coin + coin;
            parent.children.add(s);
            return;
        }
        int x = parent.positions[next_id*2];
        int y = parent.positions[next_id*2+1];

        boolean died = true;
        if (x >= 0) {
            Field newfield;
            newfield = tryMove(field, x-1, y, next_id, positions);
            if (newfield != null) {
                if (got_coin && next_id == id) coin++;
                checkTroyMove(parent, newfield, positions, queue, next_id+1, coin);
                died = false;
            }
            newfield = tryMove(field, x+1, y, next_id, positions);
            if (newfield != null) {
                if (got_coin && next_id == id) coin++;
                checkTroyMove(parent, newfield, positions, queue, next_id+1, coin);
                died = false;
            }
            newfield = tryMove(field, x, y-1, next_id, positions);
            if (newfield != null) {
                if (got_coin && next_id == id) coin++;
                checkTroyMove(parent, newfield, positions, queue, next_id+1, coin);
                died = false;
            }
            newfield = tryMove(field, x, y+1, next_id, positions);
            if (newfield != null) {
                if (got_coin && next_id == id) coin++;
                checkTroyMove(parent, newfield, positions, queue, next_id+1, coin);
                died = false;
            }
        }

        if (died && next_id != id) {
            // continue to search when enemy is died.
            // don't continue when I'm died.
            positions[next_id*2] = positions[next_id*2+1] = -1;
            checkTroyMove(parent, field, positions, queue, next_id+1, coin);
            return;
        }
    }

    private void lookAhead(State parent, ArrayDeque<State> queue)
    {
        int[] positions = (int[])parent.positions.clone();
        parent.children = new ArrayList<State>(16);
        checkTroyMove(parent, parent.field, positions, queue, 0, 0);
    }

    FieldDirection best_direction;

    int survive_count;
    private State createStateFromPlayfield(int[] prev_pos)
    {
        int[] positions = new int[numPlayers * 2];
        survive_count = numPlayers;
        for (int i = 0; i < numPlayers; ++i) {
            positions[i*2] = getXOf(i);
            positions[i*2+1] = getYOf(i);

            if (prev_pos != null) {
                if ((prev_pos[i*2] < 0) ||
                    (prev_pos[i*2] == positions[i*2] &&
                    prev_pos[i*2+1] == positions[i*2+1])) {
                    positions[i*2] = positions[i*2+1] = -1;
                    survive_count--;
                }
            }
        }
        return new State(Field.fromPlayField(playField), positions);
    }

    int[] prev_positions = null;
    public FieldDirection getInput()
    {
        State first_state = createStateFromPlayfield(prev_positions);
        prev_positions = first_state.positions;
        ArrayDeque<State> queue = new ArrayDeque<State>();
        queue.addLast(first_state);

        //while (getRemainingTime() > 90) {
        //int i = 1000;
        state_count = 0;
        while (state_count < state_count_limit) {
            //i--;
            State st = queue.pollFirst();
            if (st == null)
                break;
            lookAhead(st, queue);
        }
        //System.out.println(i);
        //System.out.println(Field.fromPlayField(playField).toString());
        System.out.print("Turn: ");
        System.out.println(playField.getTurn());
        System.out.println(getRemainingTime());
        //printPlayField();
        System.out.print(state_count);
        System.out.print(" / ");
        System.out.println(state_count_limit);
        calcRecursiveScore(first_state, 0);

        System.out.println(best_direction);

        int remtime = getRemainingTime();
        System.out.println(remtime);
        if (state_count >= state_count_limit && remtime > 92) {
            state_count_limit = (state_count_limit * 3) / 2 + 1;
        } else if (state_count >= state_count_limit && remtime > 80) {
            state_count_limit = (state_count_limit * 4) / 3 + 1;
        } else if (remtime < 60) {
            state_count_limit = (state_count_limit * 2) / 3 + 1;
        }

        System.out.print("Survive: ");
        System.out.println(survive_count);

        return best_direction;
    }

    private void printPlayField() {
        int w = playField.getWidth();
        int h = playField.getHeight();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int p;
                for (p = 0; p < numPlayers; ++p) {
                    if (getXOf(p) == x && getYOf(p) == y) {
                        System.out.print(p);
                        break;
                    }
                }
                if (p < numPlayers)
                    continue;

                switch (playField.getObjectAt(x, y)) {
                case OBJECT_FREE:
                    System.out.print(' ');
                    break;
                case OBJECT_COIN:
                    System.out.print('@');
                    break;
                case OBJECT_WALL:
                    System.out.print('#');
                    break;
                default:
                    System.out.print('*');
                    break;
                }
            }
            System.out.println();
        }
    }

    private static long getFreeSpace(int x, int y, byte[] work)
    {
        int xx,yy;
        long count = 0;
        work[y*width + x] = 1;

        int[] queue = new int[work.length*2];
        int end = 2;
        queue[0] = x;
        queue[1] = y;

        while (end > 1) {
            end -= 2;
            x = queue[end];
            y = queue[end+1];

            xx = (x + width + 1) % width;
            yy = y;
            if (work[yy*width + xx] == 0) {
                count++;
                work[yy*width + xx] = 1;
                queue[end] = xx;
                queue[end+1] = yy;
                end += 2;
            }

            xx = (x + width - 1) % width;
            yy = y;
            if (work[yy*width + xx] == 0) {
                count++;
                work[yy*width + xx] = 1;
                queue[end] = xx;
                queue[end+1] = yy;
                end += 2;
            }

            xx = x;
            yy = (y + height + 1) % height;
            if (work[yy*width + xx] == 0) {
                count++;
                work[yy*width + xx] = 1;
                queue[end] = xx;
                queue[end+1] = yy;
                end += 2;
            }

            xx = x;
            yy = (y + height - 1) % height;
            if (work[yy*width + xx] == 0) {
                count++;
                work[yy*width + xx] = 1;
                queue[end] = xx;
                queue[end+1] = yy;
                end += 2;
            }
        }

        return count;
    }

    private long _calcStandaloneScore(State s) {
        return 100;
    }
    private long calcStandaloneScore(State s)
    {
        int[] work = new int[width*height];
        int[] pos_queue = new int[width*height*4];
        int pos_end = 0;
        int pos_start = 0;
        int[] scores = new int[numPlayers];

        int offs = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                work[offs] = s.field.getAt(x, y) ? -1 : 0;
                offs++;
            }
        }
        for (int p = 0; p < numPlayers; ++p) {
            if (s.positions[p*2] < 0) continue;
            int x = s.positions[p*2];
            int y = s.positions[p*2+1];
            pos_queue[pos_end] = x;
            pos_queue[pos_end+1] = y;
            pos_end += 2;
            work[width*y + x] = p+1;
        }

        int limit = 2000;
        while (pos_start < pos_end && limit > 0) {
            limit--;
            int x = pos_queue[pos_start];
            int y = pos_queue[pos_start+1];
            int p = work[y * width + x];
            pos_start += 2;

            int xp = (x + width + 1) % width;
            int xm = (x + width - 1) % width;
            int yp = (y + height + 1) % height;
            int ym = (y + height - 1) % height;

            if (work[y*width + xp] == 0) {
                if ((work[yp*width + xp] > p+1) || (work[ym*width + xp] > p+1)) {
                    work[y * width + xp] = -2;
                } else {
                    scores[p-1]++;
                    work[y*width + xp] = p;
                    pos_queue[pos_end] = xp;
                    pos_queue[pos_end+1] = y;
                    pos_end += 2;
                }
            }

            if (work[y * width + xm] == 0) {
                if ((work[yp * width + xm] > p+1) || (work[ym * width + xm] > p+1)) {
                    work[y * width + xm] = -2;
                } else {
                    scores[p-1]++;
                    work[y * width + xm] = p;
                    pos_queue[pos_end] = xm;
                    pos_queue[pos_end+1] = y;
                    pos_end += 2;
                }
            }

            if (work[yp * width + x] == 0) {
                if ((work[yp*width + xp] > p+1) || (work[yp*width + xm] > p+1)) {
                    work[yp * width + x] = -2;
                } else {
                    scores[p-1]++;
                    work[yp * width + x] = p;
                    pos_queue[pos_end] = x;
                    pos_queue[pos_end+1] = yp;
                    pos_end += 2;
                }
            }

            if (work[ym * width + x] == 0) {
                if ((work[ym * width + xp] > p+1) || (work[ym * width + xm] > p+1)) {
                    work[ym * width + x] = -2;
                } else {
                    scores[p-1]++;
                    work[ym * width + x] = p;
                    pos_queue[pos_end] = x;
                    pos_queue[pos_end+1] = ym;
                    pos_end += 2;
                }
            }

        }

        int self_score = scores[id];
        int sum_enemy_score = 0;
        int best_enemy_score = 0;
        /*
        if (false) {
            System.out.print("position = ");
            System.out.print(s.positions[id*2]);
            System.out.print(", ");
            System.out.println(s.positions[id*2+1]);
            System.out.print(s.field.toString());
            System.out.print("free: ");
            System.out.println(self_score);
        }
        */
        for (int p = 0; p < numPlayers; ++p) {
            if (p == id)
                continue;
            sum_enemy_score = scores[p];
            if (scores[p] > best_enemy_score)
                best_enemy_score = scores[p];
        }
        int coin_score = s.coin;
        //if (survive_count == 1) coin_score *= 100;
        return self_score * (numPlayers+2) - sum_enemy_score
            - best_enemy_score*2 + coin_score * 10;
    }

    private long calcRecursiveScore(State s, int depth)
    {
        long upscore = MAX_SCORE;
        long downscore = upscore;
        long leftscore = upscore;
        long rightscore = upscore;

        if (s.positions[id*2] < 0) return MIN_SCORE; // I'm died...
        if (s.children == null)
            return calcStandaloneScore(s);
        if (s.children.isEmpty())
            return MIN_SCORE; // dead...

        for (State child : s.children) {
            FieldDirection d = child.getMovement(id, s);
            long score = calcRecursiveScore(child, depth+1) + 10; // bonus for living 1 turn.

            switch (child.getMovement(id, s)) {
            case Up:
                upscore = Math.min(upscore, score);
                break;
            case Down:
                downscore = Math.min(downscore, score);
                break;
            case Left:
                leftscore = Math.min(leftscore, score);
                break;
            case Right:
                rightscore = Math.min(rightscore, score);
                break;
            }
        }

        if (upscore == MAX_SCORE) upscore = MIN_SCORE;
        if (downscore == MAX_SCORE) downscore = MIN_SCORE;
        if (leftscore == MAX_SCORE) leftscore = MIN_SCORE;
        if (rightscore == MAX_SCORE) rightscore = MIN_SCORE;
        if (depth < 1) {
        System.out.print("Up: "); System.out.println(upscore);
        System.out.print("Down: "); System.out.println(downscore);
        System.out.print("Left: "); System.out.println(leftscore);
        System.out.print("Right: "); System.out.println(rightscore);
        }

        long bestscore = Math.max(upscore, Math.max(downscore, Math.max(leftscore, rightscore)));
        //System.out.print("Best: "); System.out.println(bestscore);

        if (upscore == bestscore) best_direction = FieldDirection.Up;
        if (downscore == bestscore) best_direction = FieldDirection.Down;
        if (leftscore == bestscore) best_direction = FieldDirection.Left;
        if (rightscore == bestscore) best_direction = FieldDirection.Right;
        //System.out.println(getRemainingTime());
        return bestscore;
    }

    /*
	public FieldDirection getInput()
	{
		int curx = getX();
		int cury = getY();
		
		List<FieldDirection> safeDirs = getSafeDirs(curx, cury);
		
		if (safeDirs.isEmpty())
		{
			//give up
			return prev_dir;
		}
		
		// Fill in your code here
		// For now, choose a one of the legal directions
		FieldDirection decision = safeDirs.get(0);
		
		prev_dir = decision;
		return decision;
	}
    */
}

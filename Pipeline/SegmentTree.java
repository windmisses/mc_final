class SegmentTree {
    SegmentTree left, right;

    long l, r;
    int cover;

    public SegmentTree(long l, long r) {
        this.l = l;
        this.r = r;
        this.cover = -1;

        this.left = null;
        this.right = null;
    }

    public SegmentTree(long l, long r, int c) {
        this.l = l;
        this.r = r;
        this.cover = c;

        this.left = null;
        this.right = null;
    }


    public void insert(int x, int y, int c) {
        if (x > this.r || y < this.l) return;

        if (this.cover == c) return;

        if (x <= this.l && this.r <= y) {
            this.cover = c;
            this.left = null;
            this.right = null;
            return;
        }

        //System.out.println(x + " " + y + " " + l + " " + r);
        
        if (this.left == null) 
            this.left = new SegmentTree(this.l, (this.l + this.r) / 2, this.cover);
        if (this.right == null) 
            this.right = new SegmentTree((this.l + this.r) / 2 + 1, this.r, this.cover); 
        this.cover = 0;
        
        this.left.insert(x, y, c);
        this.right.insert(x, y, c);

        if (this.left.cover != 0 && this.left.cover == this.right.cover) {
            this.cover = this.left.cover;
            this.left = null;
            this.right = null;
        }
    }

    public boolean find(int x) {
        //System.out.println(x + " " + l + " " + r + " " + cover);

        if (this.cover != 0) 
            return (this.cover == 1);

        if (x <= (this.l + this.r) / 2) 
            return this.left.find(x);
        else
            return this.right.find(x);
    }
}


